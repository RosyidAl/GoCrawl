package id.gocrawl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;

/**
 * Utilitas ekspor sitasi akademik standar (BibTeX .bib & Research Information Systems .ris)
 * untuk integrasi langsung ke Mendeley, Zotero, dan EndNote.
 */
public final class CitationExporter {

    private CitationExporter() {}

    /**
     * Menghasilkan string BibTeX dari kumpulan artikel.
     */
    public static String toBibTeX(Collection<Article> articles) {
        if (articles == null || articles.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Article a : articles) {
            String citeKey = generateCiteKey(a, idx++);
            sb.append("@article{").append(citeKey).append(",\n");
            appendBibField(sb, "title", a.title());
            appendBibField(sb, "author", formatBibTeXAuthors(a.authors()));
            appendBibField(sb, "journal", a.journal());
            appendBibField(sb, "year", a.year());
            if (a.doi() != null && !a.doi().isBlank()) {
                appendBibField(sb, "doi", a.doi());
            }
            if (a.url() != null && !a.url().isBlank()) {
                appendBibField(sb, "url", a.url());
            }
            if (a.abstractText() != null && !a.abstractText().isBlank()) {
                appendBibField(sb, "abstract", a.abstractText());
            }
            if (a.keywords() != null && !a.keywords().isBlank()) {
                appendBibField(sb, "keywords", a.keywords());
            }
            sb.append("}\n\n");
        }
        return sb.toString();
    }

    /**
     * Menghasilkan string format RIS dari kumpulan artikel.
     */
    public static String toRIS(Collection<Article> articles) {
        if (articles == null || articles.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Article a : articles) {
            sb.append("TY  - JOUR\n");
            if (a.title() != null && !a.title().isBlank()) {
                sb.append("TI  - ").append(cleanField(a.title())).append("\n");
            }
            if (a.authors() != null && !a.authors().isBlank()) {
                for (String author : a.authors().split(";")) {
                    String cleanAuth = author.strip();
                    if (!cleanAuth.isEmpty()) {
                        sb.append("AU  - ").append(cleanAuth).append("\n");
                    }
                }
            }
            if (a.journal() != null && !a.journal().isBlank()) {
                sb.append("JO  - ").append(cleanField(a.journal())).append("\n");
            }
            if (a.year() != null && !a.year().isBlank()) {
                sb.append("PY  - ").append(a.year().strip()).append("\n");
            }
            if (a.doi() != null && !a.doi().isBlank()) {
                sb.append("DO  - ").append(a.doi().strip()).append("\n");
            }
            if (a.url() != null && !a.url().isBlank()) {
                sb.append("UR  - ").append(a.url().strip()).append("\n");
            }
            if (a.abstractText() != null && !a.abstractText().isBlank()) {
                sb.append("AB  - ").append(cleanField(a.abstractText())).append("\n");
            }
            if (a.keywords() != null && !a.keywords().isBlank()) {
                for (String kw : a.keywords().split(",")) {
                    String cleanKw = kw.strip();
                    if (!cleanKw.isEmpty()) {
                        sb.append("KW  - ").append(cleanKw).append("\n");
                    }
                }
            }
            sb.append("ER  - \n\n");
        }
        return sb.toString();
    }

    public static void exportBibTeX(Path path, Collection<Article> articles) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(toBibTeX(articles));
        }
    }

    public static void exportRIS(Path path, Collection<Article> articles) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(toRIS(articles));
        }
    }

    private static String generateCiteKey(Article a, int fallbackIdx) {
        String authorSurname = "";
        if (a.authors() != null && !a.authors().isBlank()) {
            String first = a.authors().split(";")[0].strip();
            String[] parts = first.split("\\s+");
            authorSurname = parts[parts.length - 1].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
        String year = (a.year() != null && !a.year().isBlank()) ? a.year().strip() : "nodate";
        String firstWord = "";
        if (a.title() != null && !a.title().isBlank()) {
            String[] words = a.title().strip().split("\\s+");
            if (words.length > 0) {
                firstWord = words[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            }
        }
        if (authorSurname.isEmpty()) authorSurname = "article" + fallbackIdx;
        return authorSurname + year + (firstWord.isEmpty() ? "" : "_" + firstWord);
    }

    private static String formatBibTeXAuthors(String authors) {
        if (authors == null || authors.isBlank()) return "";
        String[] parts = authors.split(";");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String auth = parts[i].strip();
            if (auth.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" and ");
            sb.append(auth);
        }
        return sb.toString();
    }

    private static void appendBibField(StringBuilder sb, String fieldName, String value) {
        if (value == null || value.isBlank()) return;
        String cleanVal = cleanField(value).replace("}", "\\}").replace("{", "\\{");
        sb.append("  ").append(fieldName).append(" = {").append(cleanVal).append("},\n");
    }

    private static String cleanField(String val) {
        if (val == null) return "";
        return val.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").strip();
    }
}
