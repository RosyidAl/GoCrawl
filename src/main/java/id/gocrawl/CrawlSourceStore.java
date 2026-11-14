package id.gocrawl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Penyimpanan daftar sumber crawl di data/crawl_sources.txt.
 * Format per baris: enabled|builtin|type|nama|baseUrl (dipisah pipe — sengaja bukan
 * JSON agar parsingnya scanner sederhana, aman dari rekursi regex).
 * Baris lama 4 kolom (tanpa type) tetap terbaca → type = garuda.
 */
public final class CrawlSourceStore {

    private CrawlSourceStore() {}

    public static final String DEFAULT_GARUDA_URL = "https://garuda.kemdiktisaintek.go.id";

    public static List<CrawlSource> load(Path file) {
        List<CrawlSource> sources = new ArrayList<>();
        if (Files.exists(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    CrawlSource source = parseLine(line);
                    if (source != null) sources.add(source);
                }
            } catch (Exception ignored) {
                // berkas rusak — jatuh ke default di bawah
            }
        }
        if (sources.isEmpty()) {
            sources.add(new CrawlSource("Garuda", DEFAULT_GARUDA_URL, CrawlSource.TYPE_GARUDA, true, true));
        }
        return sources;
    }

    private static CrawlSource parseLine(String line) {
        if (line == null || line.isBlank() || line.startsWith("#")) return null;
        String[] parts = line.split("\\|", -1);
        try {
            if (parts.length >= 5) {
                String name = unescape(parts[3]).strip();
                String baseUrl = unescape(parts[4]).strip();
                String type = parts[2].strip().isEmpty() ? CrawlSource.TYPE_GARUDA : parts[2].strip();
                if (name.isEmpty() || baseUrl.isEmpty()) return null;
                return new CrawlSource(name, baseUrl, type,
                        Boolean.parseBoolean(parts[0].strip()),
                        Boolean.parseBoolean(parts[1].strip()));
            }
            // Format lama 4 kolom: enabled|builtin|nama|baseUrl
            if (parts.length == 4) {
                String name = unescape(parts[2]).strip();
                String baseUrl = unescape(parts[3]).strip();
                if (name.isEmpty() || baseUrl.isEmpty()) return null;
                return new CrawlSource(name, baseUrl, CrawlSource.TYPE_GARUDA,
                        Boolean.parseBoolean(parts[0].strip()),
                        Boolean.parseBoolean(parts[1].strip()));
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public static void save(Path file, List<CrawlSource> sources) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (CrawlSource source : sources) {
                w.write(Boolean.toString(source.enabled()) + "|"
                        + Boolean.toString(source.builtin()) + "|"
                        + source.type() + "|"
                        + escape(source.name()) + "|"
                        + escape(source.baseUrl()));
                w.write("\n");
            }
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static String unescape(String s) {
        return s == null ? "" : s;
    }
}
