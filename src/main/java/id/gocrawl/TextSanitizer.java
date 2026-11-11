package id.gocrawl;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utilitas pembersihan teks, sanitasi entitas HTML, perbaikan mojibake,
 * dan normalisasi DOI/judul artikel.
 */
public final class TextSanitizer {
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern TITLE_PREFIX = Pattern.compile("^(?i)(view of|tampilan)\\s+");
    private static final Pattern NOISE_PREFIX = Pattern.compile("^(?i)(po\\s+|artikel\\s*:\\s*)");
    private static final Pattern GALLEY_DOI_SUFFIX = Pattern.compile("\\.g\\d+$|/pdf.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOI_PREFIX = Pattern.compile("^(?i)(https?://(?:dx\\.)?doi\\.org/|doi:\\s*)");

    private TextSanitizer() {}

    /**
     * Membersihkan spasi ganda, karakter newline/tab, dan karakter non-breaking space.
     */
    public static String clean(String s) {
        if (s == null) return "";
        String text = s.replace('\u00a0', ' ')
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&rsquo;", "’")
                .replace("&lsquo;", "‘")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&amp;", "&");
        text = repairMojibake(text);
        return MULTI_SPACE.matcher(text).replaceAll(" ").strip();
    }

    /**
     * Membersihkan judul artikel dari prefiks artefak galley ("View of", "Tampilan"),
     * typo prefix (seperti "Po "), dan sufiks nama jurnal pada tag <title> HTML.
     */
    public static String cleanTitle(String title) {
        if (title == null) return "";
        String t = clean(title);
        t = TITLE_PREFIX.matcher(t).replaceFirst("");
        t = NOISE_PREFIX.matcher(t).replaceFirst("");

        // Bersihkan sufiks " | Nama Jurnal" jika berasal dari fallback tag <title>
        int pipeIdx = t.indexOf('|');
        if (pipeIdx > 0 && pipeIdx >= t.length() / 2) {
            t = t.substring(0, pipeIdx).strip();
        }
        return clean(t);
    }

    /**
     * Menstandarisasi string DOI: menghilangkan URL resolver (https://doi.org/),
     * prefix "doi:", dan sufiks galley (.gXXXX).
     */
    public static String cleanDoi(String rawDoi) {
        if (rawDoi == null) return "";
        String doi = rawDoi.strip();
        doi = DOI_PREFIX.matcher(doi).replaceFirst("");
        doi = GALLEY_DOI_SUFFIX.matcher(doi).replaceFirst("");
        while (doi.endsWith(".") || doi.endsWith("/")) {
            doi = doi.substring(0, doi.length() - 1);
        }
        return doi.strip();
    }

    /**
     * Memperbaiki karakter mojibake secara mendalam.
     */
    public static String repairMojibake(String text) {
        if (text == null || text.isEmpty()) return "";

        String result = text;
        // Coba perbaikan decode bertahap jika masih terdeteksi byte UTF-8 misdecoded
        for (int i = 0; i < 2; i++) {
            if (!result.contains("Ã") && !result.contains("â€") && !result.contains("Å«") && !result.contains("Å")) {
                break;
            }
            try {
                byte[] bytes = result.getBytes(StandardCharsets.ISO_8859_1);
                String candidate = new String(bytes, StandardCharsets.UTF_8);
                if (!candidate.contains("\uFFFD") && candidate.length() < result.length()) {
                    result = candidate;
                } else {
                    break;
                }
            } catch (Exception ignored) {
                break;
            }
        }

        // Penggantian token manual untuk residual mojibake
        return result.replace("â€œ", "“")
                .replace("â€", "”")
                .replace("â€™", "’")
                .replace("â€˜", "‘")
                .replace("â€”", "—")
                .replace("â€“", "–")
                .replace("Ã¢â‚¬Å“", "“")
                .replace("Ã¢â‚¬Â", "”")
                .replace("Ã¢â‚¬â„¢", "’")
                .replace("Ã¢â‚¬Ëœ", "‘")
                .replace("Ã¢â‚¬â€", "—")
                .replace("Ã¢â‚¬â€“", "–")
                .replace("Å«", "ū")
                .replace("Å", "ā")
                .replace("Ã©", "é")
                .replace("Ã¡", "á")
                .replace("Ã­", "í")
                .replace("Ã³", "ó")
                .replace("Ãº", "ú")
                .replace("Ã±", "ñ");
    }
}
