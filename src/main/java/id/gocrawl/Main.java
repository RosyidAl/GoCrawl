package id.gocrawl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Main {
    private static final HttpClient HTTP = createHttpClient();

    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
    }

    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            if (options.help) {
                usage();
                return;
            }
            List<URI> targets = readTargets(Path.of("journal.txt"));
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("journal.txt tidak berisi URL target.");
            }
            Files.createDirectories(options.output);
            int failures = 0;
            for (URI target : targets) {
                String name = journalName(target);
                System.out.println("[MULAI] " + name + " — " + target);
                try {
                    Path folder = options.output.resolve(name);
                    Files.createDirectories(folder);
                    List<Article> articles = Collections.synchronizedList(new ArrayList<>());
                    ArticleSink sink = article -> {
                        synchronized (articles) {
                            articles.add(article);
                            writeCsv(folder.resolve("articles.csv"), articles);
                            writeJson(folder.resolve("articles.json"), articles);
                            System.out.println("[COCOK] " + name + ": " + articles.size() + " — " + article.title());
                        }
                    };
                    new OjsCrawler().crawl(target, options, sink);
                    synchronized (articles) {
                        writeCsv(folder.resolve("articles.csv"), articles);
                        writeJson(folder.resolve("articles.json"), articles);
                    }
                    System.out.println("[SELESAI] " + name + ": " + articles.size() + " artikel cocok; output " + folder);
                } catch (Exception e) {
                    failures++;
                    System.err.println("[GAGAL] " + name + ": " + message(e));
                }
            }
            System.out.printf("Selesai. %d target, %d gagal.%n", targets.size(), failures);
            if (failures == targets.size()) System.exit(2);
        } catch (Exception e) {
            System.err.println("Error: " + message(e));
            usage();
            System.exit(1);
        }
    }

    private static List<URI> readTargets(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("File target tidak ditemukan: " + path.toAbsolutePath());
        }
        List<URI> result = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String value = line.strip();
            if (value.startsWith("\uFEFF")) {
                value = value.substring(1).strip();
            }
            if (value.isEmpty() || value.startsWith("#")) continue;
            URI uri = URI.create(value);
            if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("URL tidak valid di journal.txt: " + value);
            }
            result.add(uri);
        }
        return result;
    }

    public static String journalName(URI uri) {
        String[] parts = Optional.ofNullable(uri.getPath()).orElse("").split("/");
        String name = "";
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("index.php") && i + 1 < parts.length) {
                name = parts[i + 1];
                break;
            }
        }
        if (name.isBlank()) {
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isBlank()) {
                    name = parts[i];
                    break;
                }
            }
        }
        if (name.equalsIgnoreCase("index")) name = "";
        if (name.isBlank()) name = uri.getHost().split("\\.")[0];
        return name.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String message(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static void usage() {
        System.out.println("GoCrawl — crawler metadata OJS (Java 21 / Fase 2)\n" +
                "Pemakaian: java -jar gocrawl.jar --keyword <kata/frasa> [--from-year YYYY] [--to-year YYYY] [--output DIR]\n" +
                "Kueri multi-kata kunci: pisahkan dengan koma (,) atau operator OR, contoh: --keyword \"islamisasi, islamization, dakwah\"\n" +
                "Default output: output/ | Target dibaca dari journal.txt | Tahun opsional.");
    }

    public record Options(String keyword, Integer fromYear, Integer toYear, Path output, boolean help) {
        public static Options parse(String[] args) {
            String keyword = null;
            Integer from = null, to = null;
            Path output = Path.of("output");
            boolean help = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> help = true;
                    case "--keyword", "-k" -> keyword = requireValue(args, ++i, args[i - 1]);
                    case "--from-year" -> from = Integer.parseInt(requireValue(args, ++i, "--from-year"));
                    case "--to-year" -> to = Integer.parseInt(requireValue(args, ++i, "--to-year"));
                    case "--output", "-o" -> output = Path.of(requireValue(args, ++i, "--output"));
                    default -> throw new IllegalArgumentException("Argumen tidak dikenal: " + args[i]);
                }
            }
            if (help) return new Options("", null, null, output, true);
            if (keyword == null || keyword.isBlank()) throw new IllegalArgumentException("--keyword wajib diisi.");
            if (from != null && to != null && from > to) {
                throw new IllegalArgumentException("--from-year tidak boleh melebihi --to-year.");
            }
            return new Options(keyword.strip(), from, to, output, false);
        }

        private static String requireValue(String[] args, int i, String option) {
            if (i >= args.length || args[i].startsWith("--")) {
                throw new IllegalArgumentException("Nilai untuk " + option + " belum diberikan.");
            }
            return args[i];
        }
    }

    @FunctionalInterface
    public interface ArticleSink {
        void accept(Article article) throws IOException;
    }

    private static final class OjsCrawler {
        private final Set<String> visited = ConcurrentHashMap.newKeySet();
        private final DeduplicationEngine dedup = new DeduplicationEngine();
        private final List<Future<?>> articleTasks = new CopyOnWriteArrayList<>();
        private final ExecutorService articlePool = Executors.newFixedThreadPool(3);
        private final AtomicInteger pagesFetched = new AtomicInteger();
        private URI base;
        private Options options;
        private ArticleSink sink;

        public void crawl(URI target, Options options, ArticleSink sink) throws Exception {
            this.base = target;
            this.options = options;
            this.sink = sink;
            try {
                // 1. Crawl halaman awal jurnal (seringkali memuat issue/current)
                crawlIssue(target);

                // 2. Crawl issue/current secara eksplisit jika ada
                URI current = resolve(target, "issue/current");
                crawlIssue(current);

                // 3. Crawl issue/archive dan rekursi paginasinya
                URI archive = target.toString().matches(".*/issue/archive/?$") ? target : resolve(target, "issue/archive");
                crawlArchive(archive, 0);

                for (Future<?> task : articleTasks) {
                    task.get();
                }
            } finally {
                articlePool.shutdown();
                if (!articlePool.awaitTermination(30, TimeUnit.SECONDS)) {
                    articlePool.shutdownNow();
                }
            }
        }

        private void crawlArchive(URI uri, int depth) throws Exception {
            if (depth > 20 || !visited.add(uri.toString())) return;
            String html = fetch(uri);
            List<String> links = MetadataExtractor.extractLinks(html, uri);
            for (String href : links) {
                URI next = safeResolve(uri, href);
                if (next == null) continue;
                String path = next.getPath() == null ? "" : next.getPath();
                if (path.contains("/issue/view/")) {
                    crawlIssue(next);
                } else if (path.contains("/issue/archive") && !next.equals(uri)) {
                    crawlArchive(next, depth + 1);
                }
            }
        }

        private void crawlIssue(URI uri) throws Exception {
            if (!visited.add(uri.toString())) return;
            String html;
            try {
                html = fetch(uri);
            } catch (Exception e) {
                return;
            }
            List<String> links = MetadataExtractor.extractLinks(html, uri);
            for (String href : links) {
                URI candidate = safeResolve(uri, href);
                // Hanya proses URL landing page metadata artikel asli
                if (candidate != null && UrlFilter.isArticleLandingUrl(candidate)) {
                    URI canonicalUri = URI.create(UrlFilter.canonicalizeArticleUrl(candidate));
                    articleTasks.add(articlePool.submit(() -> {
                        try {
                            crawlArticle(canonicalUri);
                        } catch (Exception e) {
                            System.err.println("[ARTIKEL GAGAL] " + canonicalUri + ": " + message(e));
                        }
                    }));
                }
            }
        }

        private void crawlArticle(URI uri) throws Exception {
            String canonicalUrl = UrlFilter.canonicalizeArticleUrl(uri);
            String articleId = UrlFilter.extractArticleId(uri);
            if (!visited.add(canonicalUrl)) return;

            String html = fetch(uri);
            Article article = MetadataExtractor.extractArticle(html, uri, journalName(base));

            // Evaluasi Quality Gate (Reject ghost records, kueri filter kata kunci & rentang tahun)
            if (!QualityGate.isValid(article, options.keyword, options.fromYear, options.toYear)) {
                return;
            }

            // Evaluasi Deduplikasi Multi-Lapis
            if (dedup.isDuplicateAndRegister(article.url(), articleId, article.doi(), article.title(), article.authors(), article.year())) {
                return;
            }

            sink.accept(article);
        }

        private String fetch(URI uri) throws Exception {
            int maxRetries = 2;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25))
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                            .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                            .GET().build();
                    HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (response.statusCode() == 403 || response.statusCode() == 404) {
                        throw new IOException("HTTP " + response.statusCode() + " saat membuka " + uri);
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        if (attempt < maxRetries) {
                            Thread.sleep(600);
                            continue;
                        }
                        throw new IOException("HTTP " + response.statusCode() + " saat membuka " + uri);
                    }
                    int count = pagesFetched.incrementAndGet();
                    if (count == 1 || count % 5 == 0) {
                        System.out.println("[PROGRES] " + journalName(base) + ": " + count + " halaman; " + uri);
                    }
                    Thread.sleep(200);
                    return response.body();
                } catch (Exception e) {
                    if (attempt < maxRetries && isRetryable(e)) {
                        Thread.sleep(800);
                        continue;
                    }
                    throw e;
                }
            }
            throw new IOException("Gagal membuka " + uri);
        }

        private boolean isRetryable(Throwable e) {
            String m = (e.getMessage() == null ? "" : e.getMessage()).toLowerCase(Locale.ROOT);
            return m.contains("goaway") || m.contains("timeout") || m.contains("reset")
                    || m.contains("closed") || m.contains("eof") || e instanceof java.net.SocketTimeoutException;
        }

        private URI safeResolve(URI from, String href) {
            try {
                URI u = from.resolve(href);
                if (!Set.of("http", "https").contains(u.getScheme()) || !Objects.equals(u.getHost(), base.getHost())) {
                    return null;
                }
                return u;
            } catch (Exception e) {
                return null;
            }
        }

        private URI resolve(URI root, String relative) {
            String path = root.getPath().replaceAll("/+$", "").replaceFirst("(?i)/index$", "");
            return URI.create(root.getScheme() + "://" + root.getAuthority() + path + "/" + relative);
        }
    }

    private static void writeCsv(Path path, List<Article> rows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("title,authors,year,journal,url,abstract,doi,published,keywords");
            w.newLine();
            for (Article a : rows) {
                w.write(String.join(",",
                        csv(a.title()),
                        csv(a.authors()),
                        csv(a.year()),
                        csv(a.journal()),
                        csv(a.url()),
                        csv(a.abstractText()),
                        csv(a.doi()),
                        csv(a.published()),
                        csv(a.keywords())));
                w.newLine();
            }
        }
    }

    private static String csv(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    private static void writeJson(Path path, List<Article> rows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("[\n");
            for (int i = 0; i < rows.size(); i++) {
                Article a = rows.get(i);
                w.write("  {\"title\":\"" + json(a.title()) +
                        "\",\"authors\":\"" + json(a.authors()) +
                        "\",\"year\":\"" + json(a.year()) +
                        "\",\"journal\":\"" + json(a.journal()) +
                        "\",\"url\":\"" + json(a.url()) +
                        "\",\"abstract\":\"" + json(a.abstractText()) +
                        "\",\"doi\":\"" + json(a.doi()) +
                        "\",\"published\":\"" + json(a.published()) +
                        "\",\"keywords\":\"" + json(a.keywords()) + "\"}");
                w.write(i + 1 == rows.size() ? "\n" : ",\n");
            }
            w.write("]\n");
        }
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 32) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }
}
