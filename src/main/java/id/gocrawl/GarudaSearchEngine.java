package id.gocrawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mesin penelusuran artikel ilmiah langsung melalui agregator nasional Portal Garuda
 * (garuda.kemdiktisaintek.go.id/documents).
 *
 * Mengambil metadata primer artikel (Judul, Penulis, Jurnal, Penerbit, Tahun, Abstrak,
 * Tautan Asal OJS, DOI, dan metadata pendukung) secara terstruktur dan efisien.
 */
public final class GarudaSearchEngine {

    public static final String DEFAULT_BASE_URL = "https://garuda.kemdiktisaintek.go.id";
    private final String baseUrl;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_SECONDS = 8;

    // Garuda memblokir akses (HTTP 403) bila diminta beruntun terlalu cepat:
    // beri jeda minimum antar-request dan ulangi dengan backoff saat kena pembatasan
    private static final long MIN_REQUEST_SPACING_MILLIS = 1200;
    private static final int MAX_FETCH_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 4000;

    private long lastRequestMillis = 0;

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b");
    private static final Pattern DOI_PATTERN = Pattern.compile("10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+");
    private static final Pattern PAGINATION_PATTERN = Pattern.compile("Page\\s+(\\d+)\\s+of\\s+(\\d+)\\s*\\|\\s*Total Record\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    public GarudaSearchEngine() {
        this(DEFAULT_BASE_URL);
    }

    public GarudaSearchEngine(String baseUrl) {
        this(baseUrl, createHttpClient());
    }

    public GarudaSearchEngine(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
    }

    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());

            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(ctx)
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
    }

    public record SearchOptions(
            String keyword,
            String subjectScopeId,
            String minSintaRank,
            Integer fromYear,
            Integer toYear,
            int page
    ) {
        public static SearchOptions of(String keyword) {
            return new SearchOptions(keyword, "ALL", "ALL", null, null, 1);
        }
    }

    public record PageResult(
            List<Article> articles,
            int currentPage,
            int totalPages,
            int totalRecords,
            boolean hasMore
    ) {}

    /**
     * Menjalankan kueri pencarian dokumen ke Portal Garuda untuk satu halaman tertentu.
     * Diberi jeda minimum antar-request dan retry ber-backoff bila kena pembatasan (403/429/5xx).
     */
    public synchronized PageResult searchPage(SearchOptions options, JournalCatalog catalog) throws IOException, InterruptedException {
        if (options.keyword() == null || options.keyword().isBlank()) {
            return new PageResult(Collections.emptyList(), 0, 0, 0, false);
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl).append("/documents?q=")
                .append(URLEncoder.encode(options.keyword().strip(), StandardCharsets.UTF_8));

        if (options.fromYear() != null) {
            urlBuilder.append("&from=").append(options.fromYear());
        }
        if (options.toYear() != null) {
            urlBuilder.append("&to=").append(options.toYear());
        }
        if (options.page() > 1) {
            urlBuilder.append("&page=").append(options.page());
        }

        String targetUrl = urlBuilder.toString();
        HttpRequest request = HttpRequest.newBuilder(URI.create(targetUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        long elapsed = System.currentTimeMillis() - lastRequestMillis;
        if (elapsed < MIN_REQUEST_SPACING_MILLIS) {
            Thread.sleep(MIN_REQUEST_SPACING_MILLIS - elapsed);
        }

        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
            lastRequestMillis = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code == 200) {
                return parseDocumentPage(response.body(), options, catalog);
            }
            lastFailure = new IOException("HTTP " + code + " dari Portal Garuda: " + targetUrl);
            boolean retryable = code == 403 || code == 429 || code >= 500;
            if (!retryable || attempt == MAX_FETCH_ATTEMPTS) {
                if (code == 403) {
                    throw new IOException("Garuda membatasi akses (HTTP 403) setelah "
                            + MAX_FETCH_ATTEMPTS + " percobaan. Tunggu beberapa menit lalu coba lagi.");
                }
                throw lastFailure;
            }
            Thread.sleep(RETRY_BACKOFF_MILLIS * attempt); // 4s, 8s
        }
        throw lastFailure;
    }

    /**
     * Melakukan parsing HTML halaman hasil pencarian Portal Garuda menjadi daftar objek Article.
     */
    public PageResult parseDocumentPage(String html, SearchOptions options, JournalCatalog catalog) {
        Document doc = Jsoup.parse(html, baseUrl);

        int currentPage = options.page();
        int totalPages = 1;
        int totalRecords = 0;

        Element paginationInfo = doc.selectFirst("p.pagination-info");
        if (paginationInfo != null) {
            Matcher m = PAGINATION_PATTERN.matcher(paginationInfo.text());
            if (m.find()) {
                currentPage = Integer.parseInt(m.group(1));
                totalPages = Integer.parseInt(m.group(2));
                totalRecords = Integer.parseInt(m.group(3));
            }
        }

        Elements items = doc.select(".article-item");
        List<Article> articles = new ArrayList<>();
        Set<String> seenOnPage = new HashSet<>();

        for (Element item : items) {
            Article article = parseArticleItem(item, catalog);
            if (article == null || article.title().isBlank()) continue;

            // Evaluasi Rentang Tahun jika ditentukan
            if (options.fromYear() != null || options.toYear() != null) {
                if (!article.year().equals("-")) {
                    try {
                        int y = Integer.parseInt(article.year());
                        if (options.fromYear() != null && y < options.fromYear()) continue;
                        if (options.toYear() != null && y > options.toYear()) continue;
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Evaluasi Filter Akreditasi SINTA jika ditentukan
            if (catalog != null && options.minSintaRank() != null && !options.minSintaRank().equalsIgnoreCase("ALL")) {
                String sRank = catalog.resolveSintaRank(article.url(), article.journal());
                if (!JournalCatalog.matchesSinta(sRank, options.minSintaRank())) {
                    continue; // Peringkat SINTA tidak memenuhi syarat
                }
            }

            // Evaluasi Filter Bidang Ilmu jika ditentukan
            if (catalog != null && options.subjectScopeId() != null && !options.subjectScopeId().isBlank() && !options.subjectScopeId().equalsIgnoreCase("ALL")) {
                Journal j = catalog.findByUrlOrHost(article.url());
                if (j == null && article.journal() != null) {
                    j = catalog.findByName(article.journal());
                }
                String targetId = GarudaSubject.resolveSubjectId(options.subjectScopeId());
                if (j != null) {
                    boolean match = false;
                    if (targetId != null) {
                        for (String part : j.subjectId().split(",")) {
                            if (part.trim().equals(targetId)) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (!match) {
                        String q = options.subjectScopeId().toLowerCase(Locale.ROOT).trim();
                        if (j.subjectName() != null && j.subjectName().toLowerCase(Locale.ROOT).contains(q)) {
                            match = true;
                        } else if (j.coreSubject() != null && j.coreSubject().toLowerCase(Locale.ROOT).contains(q)) {
                            match = true;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                }
            }

            // Cegah duplikasi artikel dalam halaman yang sama
            String dedupKey = article.url().isBlank() ? article.title().toLowerCase(Locale.ROOT) : article.url();
            if (!seenOnPage.add(dedupKey)) {
                continue;
            }

            articles.add(article);
        }

        boolean hasMore = currentPage < totalPages && !items.isEmpty();
        return new PageResult(articles, currentPage, totalPages, totalRecords, hasMore);
    }

    /**
     * Mengekstrak satu elemen `.article-item` menjadi entitas Article lengkap.
     */
    public Article parseArticleItem(Element item, JournalCatalog catalog) {
        if (item == null) return null;

        // 1. Judul Artikel
        Element titleEl = item.selectFirst("a.title-article");
        String rawTitle = "";
        if (titleEl != null) {
            Element xmp = titleEl.selectFirst("xmp");
            rawTitle = xmp != null ? xmp.text() : titleEl.text();
        }
        String title = cleanGarudaTitle(rawTitle);
        if (title.isBlank()) return null;

        // 2. Daftar Penulis
        Elements authorEls = item.select("a.author-article");
        String authors = authorEls.stream()
                .map(Element::text)
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("; "));
        if (authors.isBlank()) authors = "-";

        // 3. Subtitle: Nama Jurnal, Volume, Nomor, Tahun
        String journalName = "-";
        String year = "-";
        Elements subtitleEls = item.select(".subtitle-article");
        if (!subtitleEls.isEmpty()) {
            String firstSub = subtitleEls.first().text().strip();
            year = extractYear(firstSub);
            journalName = extractJournalName(firstSub);
        }

        // 4. Asal Penerbit (Publisher)
        String publisher = "-";
        for (int i = 0; i < subtitleEls.size(); i++) {
            Element sub = subtitleEls.get(i);
            if (sub.text().toLowerCase(Locale.ROOT).contains("publisher")) {
                if (i + 1 < subtitleEls.size()) {
                    publisher = subtitleEls.get(i + 1).text().strip();
                    break;
                }
            }
        }

        // 5. Tautan & DOI
        String url = "";
        String doi = "";

        Element viewEl = item.selectFirst("a.title-citation[href*=/article/view/]");
        if (viewEl != null) {
            url = viewEl.attr("href").strip();
        } else if (titleEl != null && titleEl.hasAttr("href")) {
            String href = titleEl.attr("href").strip();
            if (href.startsWith("/")) {
                url = baseUrl + href;
            } else {
                url = href;
            }
        }

        Element doiEl = item.selectFirst("a.title-citation[href*=doi.org], a.title-citation:contains(DOI:)");
        if (doiEl != null) {
            String href = doiEl.attr("href");
            Matcher m = DOI_PATTERN.matcher(href.isEmpty() ? doiEl.text() : href);
            if (m.find()) {
                doi = m.group();
            }
        }

        // 6. Abstrak Artikel
        String abstractText = "";
        Element absEl = item.selectFirst(".abstract-article xmp, .abstract-article, .article-abstract xmp, .article-abstract");
        if (absEl != null) {
            String rawAbs = absEl.text().strip();
            // Hilangkan kata "Abstract" jika berada di awal teks
            rawAbs = rawAbs.replaceFirst("(?i)^abstract\\s*", "").strip();
            abstractText = TextSanitizer.clean(rawAbs);
        }

        // 7. Pengayaan dari Master Catalog lokal jika tersedia
        if (catalog != null) {
            if (publisher.equals("-") || publisher.isBlank()) {
                publisher = catalog.resolveOrigin(url, journalName);
            }
        }

        String published = year.equals("-") ? "" : year;
        String keywords = "";

        return new Article(title, authors, year, journalName, url, abstractText, doi, published, keywords);
    }

    public static String extractYear(String text) {
        if (text == null) return "-";
        Matcher m = YEAR_PATTERN.matcher(text);
        String lastYear = "-";
        while (m.find()) {
            lastYear = m.group(1);
        }
        return lastYear;
    }

    public static String extractJournalName(String text) {
        if (text == null || text.isBlank()) return "-";
        String clean = text.replaceAll("(?i)\\bVol(?:ume)?\\.?.*", "");
        clean = clean.replaceAll("(?i)\\bNo\\.?.*", "");
        clean = clean.replaceAll("\\(\\d{4}\\).*", "");
        clean = clean.replaceAll("(?i):\\s*Edisi.*", "");
        clean = clean.strip();
        return clean.isEmpty() ? text.strip() : clean;
    }

    public static String cleanGarudaTitle(String raw) {
        if (raw == null) return "";
        String s = TextSanitizer.clean(raw);
        if (s.isBlank()) return "";

        // 1. Hilangkan rogue single letter di awal (seperti "a Peran Supervisi...")
        s = s.replaceFirst("^[a-zA-Z]\\s+(?=[A-Z])", "");

        // 2. Hilangkan repetisi tepat yang dipisahkan tanda titik dua ":"
        int colonIdx = s.indexOf(':');
        if (colonIdx > 0 && colonIdx < s.length() - 1) {
            String left = s.substring(0, colonIdx).strip();
            String right = s.substring(colonIdx + 1).strip();
            if (left.equalsIgnoreCase(right) || right.toLowerCase(Locale.ROOT).startsWith(left.toLowerCase(Locale.ROOT))) {
                s = left;
            }
        }

        // 3. Hilangkan repetisi berulang frase judul (termasuk yang terpotong di batas karakter database)
        for (int phraseLen = 15; phraseLen <= s.length() / 2; phraseLen++) {
            String phrase = s.substring(0, phraseLen).strip();
            if (phrase.split("\\s+").length >= 2) {
                String doublePhrase = phrase + " " + phrase;
                if (s.toLowerCase(Locale.ROOT).startsWith(doublePhrase.toLowerCase(Locale.ROOT))) {
                    String rest = s;
                    boolean fullyMatches = true;
                    while (rest.length() > phrase.length()) {
                        if (rest.toLowerCase(Locale.ROOT).startsWith(phrase.toLowerCase(Locale.ROOT))) {
                            rest = rest.substring(phrase.length()).stripLeading();
                        } else {
                            if (!phrase.toLowerCase(Locale.ROOT).startsWith(rest.toLowerCase(Locale.ROOT))) {
                                fullyMatches = false;
                            }
                            break;
                        }
                    }
                    if (fullyMatches) {
                        s = phrase;
                        break;
                    }
                }
            }
        }

        // 4. Hilangkan fragmen awal pendek yang berulang di sisa judul
        //    (mis. "Urgensi Teknologi URGENSI TEKNOLOGI DALAM ..." → "URGENSI TEKNOLOGI DALAM ...")
        String[] words = s.split("\\s+");
        for (int take = Math.min(4, words.length - 1); take >= 1; take--) {
            String fragment = String.join(" ", Arrays.copyOfRange(words, 0, take)).strip();
            String rest = String.join(" ", Arrays.copyOfRange(words, take, words.length)).strip();
            if (rest.length() > fragment.length() * 2
                    && rest.toLowerCase(Locale.ROOT).startsWith(fragment.toLowerCase(Locale.ROOT))) {
                s = rest;
                break;
            }
        }

        // 5. Deduplikasi segmen setelah ':' yang berulang
        //    (mis. ": Pemanfaatan X, Urgensi Pemanfaatan X" → ": Pemanfaatan X")
        int subtitleIdx = s.indexOf(": ");
        if (subtitleIdx > 0 && subtitleIdx < s.length() - 2) {
            String head = s.substring(0, subtitleIdx).strip();
            String tail = s.substring(subtitleIdx + 2).strip();
            String[] segments = tail.split(",\\s*");
            List<String> kept = new ArrayList<>();
            boolean removed = false;
            for (String segment : segments) {
                String norm = segment.toLowerCase(Locale.ROOT).replaceFirst("^urgensi\\s+", "").strip();
                boolean dup = kept.stream().anyMatch(k -> {
                    String knorm = k.toLowerCase(Locale.ROOT).replaceFirst("^urgensi\\s+", "").strip();
                    return knorm.equalsIgnoreCase(norm)
                            || (knorm.length() >= 10 && norm.startsWith(knorm))
                            || (norm.length() >= 10 && knorm.startsWith(norm));
                });
                if (dup) {
                    removed = true;
                } else {
                    kept.add(segment);
                }
            }
            if (removed && !kept.isEmpty()) {
                s = head + ": " + String.join(", ", kept);
            }
        }

        return TextSanitizer.cleanTitle(s);
    }
}
