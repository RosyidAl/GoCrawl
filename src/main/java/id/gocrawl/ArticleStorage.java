package id.gocrawl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Penyimpanan dan pembacaan berkas data artikel lokal (articles.json & articles.csv)
 * untuk sinkronisasi workspace GoCrawl.
 */
public final class ArticleStorage {

    private ArticleStorage() {}

    /**
     * Memindai folder output/ dan memuat seluruh artikel yang tersimpan sebelumnya.
     */
    public static List<Article> scanWorkspace(Path outputDir) {
        List<Article> list = new ArrayList<>();
        if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
            return list;
        }

        try (var stream = Files.walk(outputDir)) {
            stream.filter(p -> p.getFileName().toString().equalsIgnoreCase("articles.json"))
                    .forEach(jsonFile -> {
                        try {
                            String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
                            list.addAll(parseJson(content));
                        } catch (Exception ignored) {}
                    });
        } catch (IOException ignored) {}

        return list;
    }

    /**
     * Parser JSON ringan khusus skema Article tanpa dependensi eksternal.
     */
    public static List<Article> parseJson(String json) {
        List<Article> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;

        // Pisahkan objek JSON per {...}
        int len = json.length();
        int braceDepth = 0;
        int start = -1;

        for (int i = 0; i < len; i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceDepth == 0) start = i;
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0 && start != -1) {
                    String objStr = json.substring(start + 1, i);
                    Map<String, String> fields = extractFields(objStr);
                    String title = fields.getOrDefault("title", "");
                    if (!title.isBlank()) {
                        list.add(new Article(
                                title,
                                fields.getOrDefault("authors", ""),
                                fields.getOrDefault("year", ""),
                                fields.getOrDefault("journal", ""),
                                fields.getOrDefault("url", ""),
                                fields.getOrDefault("abstract", fields.getOrDefault("abstractText", "")),
                                fields.getOrDefault("doi", ""),
                                fields.getOrDefault("published", ""),
                                fields.getOrDefault("keywords", "")
                        ));
                    }
                    start = -1;
                }
            }
        }
        return list;
    }

    /**
     * Pembaca pasangan "kunci": "nilai" dengan scanner karakter biasa — TANPA regex.
     * Regex dengan grup bersarang merekrusi per karakter dan meledak StackOverflowError
     * pada nilai panjang (mis. abstrak artikel).
     */
    private static Map<String, String> extractFields(String body) {
        Map<String, String> fields = new HashMap<>();
        int len = body.length();
        int i = 0;
        while (i < len) {
            char c = body.charAt(i);
            if (c != '"') {
                i++;
                continue;
            }
            // Baca nama kunci (tangani escape)
            StringBuilder key = new StringBuilder();
            i++;
            boolean keyClosed = false;
            while (i < len) {
                char kc = body.charAt(i);
                if (kc == '\\' && i + 1 < len) {
                    key.append(body.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (kc == '"') {
                    i++;
                    keyClosed = true;
                    break;
                }
                key.append(kc);
                i++;
            }
            if (!keyClosed) break;
            // Lewati hingga ':' lalu pembuka nilai
            while (i < len && body.charAt(i) != ':') i++;
            if (i >= len) break;
            i++;
            while (i < len && Character.isWhitespace(body.charAt(i))) i++;
            if (i >= len || body.charAt(i) != '"') continue;
            // Baca nilai string (tangani escape \n \r \t \" \\ dan unicode escape)
            StringBuilder value = new StringBuilder();
            i++;
            boolean valueClosed = false;
            while (i < len) {
                char vc = body.charAt(i);
                if (vc == '\\' && i + 1 < len) {
                    char next = body.charAt(i + 1);
                    if (next == 'u' && i + 5 < len) {
                        try {
                            value.append((char) Integer.parseInt(body.substring(i + 2, i + 6), 16));
                            i += 6;
                            continue;
                        } catch (NumberFormatException ignored) {
                            // bukan escape unicode valid — jatuh ke penanganan biasa
                        }
                    }
                    value.append(switch (next) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> next;
                    });
                    i += 2;
                    continue;
                }
                if (vc == '"') {
                    i++;
                    valueClosed = true;
                    break;
                }
                value.append(vc);
                i++;
            }
            if (valueClosed) {
                fields.put(key.toString(), value.toString());
            }
        }
        return fields;
    }

    public static void saveJson(Path file, Collection<Article> articles) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("[\n");
            int i = 0;
            int total = articles.size();
            for (Article a : articles) {
                w.write("  {\n");
                w.write("    \"title\": \"" + escapeJson(a.title()) + "\",\n");
                w.write("    \"authors\": \"" + escapeJson(a.authors()) + "\",\n");
                w.write("    \"year\": \"" + escapeJson(a.year()) + "\",\n");
                w.write("    \"journal\": \"" + escapeJson(a.journal()) + "\",\n");
                w.write("    \"url\": \"" + escapeJson(a.url()) + "\",\n");
                w.write("    \"abstract\": \"" + escapeJson(a.abstractText()) + "\",\n");
                w.write("    \"doi\": \"" + escapeJson(a.doi()) + "\",\n");
                w.write("    \"published\": \"" + escapeJson(a.published()) + "\",\n");
                w.write("    \"keywords\": \"" + escapeJson(a.keywords()) + "\"\n");
                w.write("  }" + (++i < total ? ",\n" : "\n"));
            }
            w.write("]\n");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

}
