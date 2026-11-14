package id.gocrawl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static Map<String, String> extractFields(String body) {
        Map<String, String> fields = new HashMap<>();
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2);
            fields.put(key, unescapeJson(val));
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

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
