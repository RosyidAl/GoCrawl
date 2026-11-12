package id.gocrawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mesin resolusi akreditasi SINTA (sinta.kemdiktisaintek.go.id) langsung dan on-demand.
 * Menghubungkan metadata artikel/jurnal dengan peringkat akreditasi SINTA (S1 s.d. S6, Scopus)
 * secara cerdas dilengkapi persistent cache lokal.
 */
public final class SintaResolver {

    private static final String SINTA_BASE_URL = "https://sinta.kemdiktisaintek.go.id/journals?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 3500;
    private static final Pattern SINTA_RANK_PATTERN = Pattern.compile("(?i)\\b(S[1-6]|Scopus(?:\\s+[Qq][1-4])?)\\b");
    private static final Path CACHE_FILE = Path.of("data", "sinta_cache.json");

    private static final SintaResolver INSTANCE = new SintaResolver();

    private final Map<String, SintaInfo> cache = new ConcurrentHashMap<>();

    public record SintaInfo(
            String queryKey,
            String journalName,
            String sintaRank,
            String publisher,
            String ojsUrl,
            String garudaId
    ) {}

    public static SintaResolver getInstance() {
        return INSTANCE;
    }

    private SintaResolver() {
        loadCache();
    }

    /**
     * Resolusi peringkat SINTA untuk nama jurnal tertentu.
     * Cek cache lokal terlebih dahulu; jika belum ada, tembak live ke SINTA dan simpan ke cache.
     */
    public String resolveRank(String journalName, String url) {
        if (journalName == null || journalName.isBlank() || journalName.equals("-")) {
            return "-";
        }

        String cleanName = cleanJournalName(journalName);
        String key = normalizeKey(cleanName);
        if (key.isBlank()) return "-";

        // 1. Cek Memory Cache
        SintaInfo cached = cache.get(key);
        if (cached != null) {
            return cached.sintaRank();
        }

        // 2. Query Live SINTA
        SintaInfo fetched = fetchFromSinta(cleanName, url);
        if (fetched != null) {
            cache.put(key, fetched);
            saveCache();
            return fetched.sintaRank();
        }

        // Simpan marker negatif agar tidak query berulang untuk jurnal non-terakreditasi
        cache.put(key, new SintaInfo(key, cleanName, "-", "-", "-", "-"));
        saveCache();
        return "-";
    }

    public SintaInfo resolveInfo(String journalName, String url) {
        if (journalName == null || journalName.isBlank() || journalName.equals("-")) {
            return null;
        }

        String cleanName = cleanJournalName(journalName);
        String key = normalizeKey(cleanName);
        if (key.isBlank()) return null;

        SintaInfo cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        SintaInfo fetched = fetchFromSinta(cleanName, url);
        if (fetched != null) {
            cache.put(key, fetched);
            saveCache();
            return fetched;
        }

        cache.put(key, new SintaInfo(key, cleanName, "-", "-", "-", "-"));
        saveCache();
        return null;
    }

    private SintaInfo fetchFromSinta(String cleanName, String optionalUrl) {
        try {
            // Gunakan query pendek bersih: ambil frasa utama sebelum pemisah judul sekunder (titik dua, dash, kurung)
            String searchQuery = cleanName.replaceAll("[:–—].*", "").replaceAll("\\(.*\\)", "").replaceAll("(?i)\\bjurnal\\b|\\bjournal\\b", "").strip();
            if (searchQuery.length() < 3) searchQuery = cleanName.replaceAll("[:–—].*", "").strip();
            if (searchQuery.length() < 3) searchQuery = cleanName;

            String sintaUrl = SINTA_BASE_URL + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(sintaUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            Elements items = doc.select(".col-lg.meta-side, .affil-item, div.row:has(.affil-name)");
            if (items.isEmpty()) {
                items = doc.select("div:has(.affil-name)");
            }

            for (Element item : items) {
                Element nameEl = item.selectFirst(".affil-name a");
                if (nameEl == null) continue;

                String sintaJournalName = nameEl.text().strip();
                if (isNameMatch(cleanName, sintaJournalName)) {
                    // Ekstraksi Akreditasi
                    String rank = "-";
                    Element accEl = item.selectFirst(".num-stat.accredited, .stat-prev:contains(Accredited), .stat-prev:contains(S)");
                    if (accEl != null) {
                        Matcher m = SINTA_RANK_PATTERN.matcher(accEl.text());
                        if (m.find()) {
                            rank = m.group(1).toUpperCase(Locale.ROOT);
                        }
                    }

                    // Ekstraksi Penerbit
                    Element pubEl = item.selectFirst(".affil-loc a, .affil-loc");
                    String publisher = pubEl != null ? pubEl.text().strip() : "-";

                    // Ekstraksi Tautan OJS
                    Element webEl = item.selectFirst("a:contains(Website)");
                    String ojsUrl = webEl != null ? webEl.attr("href").strip() : "-";

                    // Ekstraksi ID Garuda
                    Element garudaEl = item.selectFirst("a[href*=/journal/view/]");
                    String garudaId = "-";
                    if (garudaEl != null) {
                        Matcher gm = Pattern.compile("view/(\\d+)").matcher(garudaEl.attr("href"));
                        if (gm.find()) garudaId = gm.group(1);
                    }

                    return new SintaInfo(normalizeKey(cleanName), sintaJournalName, rank, publisher, ojsUrl, garudaId);
                }
            }
        } catch (Exception ignored) {
            // Jaringan timeout atau SINTA sementara down
        }
        return null;
    }

    private static boolean isNameMatch(String target, String candidate) {
        String nTarget = normalizeKey(target);
        String nCandidate = normalizeKey(candidate);
        if (nTarget.isEmpty() || nCandidate.isEmpty()) return false;

        if (nTarget.equals(nCandidate)) return true;

        // Cocokkan jika salah satu memuat yang lain (minimal panjang 5 karakter)
        if (nTarget.length() >= 6 && nCandidate.contains(nTarget)) return true;
        if (nCandidate.length() >= 6 && nTarget.contains(nCandidate)) return true;

        // Cek bagian sebelum titik dua/tanda hubung jika ada
        String targetPrefix = normalizeKey(target.replaceAll("[:–—].*", ""));
        String candidatePrefix = normalizeKey(candidate.replaceAll("[:–—].*", ""));
        if (!targetPrefix.isEmpty() && targetPrefix.equals(candidatePrefix)) return true;

        // Cek tanpa kata 'jurnal' / 'journal'
        String strippedTarget = nTarget.replaceAll("^(?:jurnal|journal)", "");
        String strippedCandidate = nCandidate.replaceAll("^(?:jurnal|journal)", "");
        if (strippedTarget.length() >= 5 && (strippedCandidate.contains(strippedTarget) || strippedTarget.contains(strippedCandidate))) {
            return true;
        }

        return false;
    }

    public static String cleanJournalName(String text) {
        if (text == null) return "";
        String clean = text.replaceAll("(?i)\\bVol(?:ume)?\\.?.*", "");
        clean = clean.replaceAll("(?i)\\bNo\\.?.*", "");
        clean = clean.replaceAll("\\(\\d{4}\\).*", "");
        clean = clean.replaceAll("(?i):\\s*Edisi.*", "");
        return clean.strip();
    }

    public static String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[^a-z0-9]", "")
                .strip();
    }

    private synchronized void loadCache() {
        if (!Files.exists(CACHE_FILE)) return;
        try {
            String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            Pattern itemPattern = Pattern.compile("\\{([^{}]+)\\}");
            Matcher m = itemPattern.matcher(json);
            while (m.find()) {
                String body = m.group(1);
                Map<String, String> map = new HashMap<>();
                Matcher fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
                while (fieldMatcher.find()) {
                    map.put(fieldMatcher.group(1), fieldMatcher.group(2));
                }
                String key = map.get("queryKey");
                if (key != null && !key.isBlank()) {
                    cache.put(key, new SintaInfo(
                            key,
                            map.getOrDefault("journalName", "-"),
                            map.getOrDefault("sintaRank", "-"),
                            map.getOrDefault("publisher", "-"),
                            map.getOrDefault("ojsUrl", "-"),
                            map.getOrDefault("garudaId", "-")
                    ));
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void saveCache() {
        try {
            if (CACHE_FILE.getParent() != null) {
                Files.createDirectories(CACHE_FILE.getParent());
            }
            StringBuilder sb = new StringBuilder("[\n");
            int i = 0;
            int total = cache.size();
            for (SintaInfo info : cache.values()) {
                sb.append("  {\n");
                sb.append("    \"queryKey\": \"").append(escape(info.queryKey())).append("\",\n");
                sb.append("    \"journalName\": \"").append(escape(info.journalName())).append("\",\n");
                sb.append("    \"sintaRank\": \"").append(escape(info.sintaRank())).append("\",\n");
                sb.append("    \"publisher\": \"").append(escape(info.publisher())).append("\",\n");
                sb.append("    \"ojsUrl\": \"").append(escape(info.ojsUrl())).append("\",\n");
                sb.append("    \"garudaId\": \"").append(escape(info.garudaId())).append("\"\n");
                sb.append("  }").append(++i < total ? ",\n" : "\n");
            }
            sb.append("]\n");
            Files.writeString(CACHE_FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
