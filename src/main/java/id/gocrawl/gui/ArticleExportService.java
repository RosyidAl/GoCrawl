package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.JournalCatalog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/** Menulis format ekspor tabular yang dipilih dari tabel artikel. */
final class ArticleExportService {

    enum Format { CSV, JSON }

    private ArticleExportService() {
    }

    static void export(Path file, Format format, Collection<Article> articles, JournalCatalog catalog)
            throws IOException {
        switch (format) {
            case CSV -> writeCsv(file, articles, catalog);
            case JSON -> writeJson(file, articles, catalog);
        }
    }

    private static void writeCsv(Path file, Collection<Article> articles, JournalCatalog catalog)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("\"Title\",\"Journal\",\"Origin/Publisher\",\"Authors\",\"Year\",\"DOI\",\"URL\",\"Abstract\"\n");
            for (Article article : articles) {
                String origin = catalog != null ? catalog.resolveOrigin(article.url(), article.journal()) : "-";
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        csv(article.title()),
                        csv(article.journal()),
                        csv(origin),
                        csv(article.authors()),
                        csv(article.year()),
                        csv(article.doi()),
                        csv(article.url()),
                        csv(article.abstractText())));
            }
        }
    }

    private static void writeJson(Path file, Collection<Article> articles, JournalCatalog catalog)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("[\n");
            int index = 0;
            for (Article article : articles) {
                String origin = catalog != null ? catalog.resolveOrigin(article.url(), article.journal()) : "-";
                writer.write("  {\n");
                writer.write("    \"title\": \"" + json(article.title()) + "\",\n");
                writer.write("    \"journal\": \"" + json(article.journal()) + "\",\n");
                writer.write("    \"origin\": \"" + json(origin) + "\",\n");
                writer.write("    \"authors\": \"" + json(article.authors()) + "\",\n");
                writer.write("    \"year\": \"" + json(article.year()) + "\",\n");
                writer.write("    \"doi\": \"" + json(article.doi()) + "\",\n");
                writer.write("    \"url\": \"" + json(article.url()) + "\",\n");
                writer.write("    \"abstract\": \"" + json(article.abstractText()) + "\"\n");
                writer.write("  }" + (++index < articles.size() ? ",\n" : "\n"));
            }
            writer.write("]\n");
        }
    }

    private static String csv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
