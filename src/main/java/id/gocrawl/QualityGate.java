package id.gocrawl;

import java.util.Arrays;
import java.util.Locale;

/**
 * Filter validasi integritas data (Quality Gate):
 * Menolak ghost record, entitas galley tanpa metadata,
 * serta memastikan kepatuhan filter kueri kata kunci dan rentang tahun.
 */
public final class QualityGate {

    private QualityGate() {}

    /**
     * Memvalidasi apakah artikel layak disimpan berdasarkan kelengkapan skema
     * dan kecocokan terhadap parameter kueri.
     */
    public static boolean isValid(Article article, String keyword, Integer fromYear, Integer toYear) {
        if (article == null) return false;

        // 1. Judul wajib tersedia dan bukan artefak kosong
        String title = article.title();
        if (title == null || title.isBlank()) return false;
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        if (lowerTitle.equals("view of") || lowerTitle.equals("tampilan")) return false;

        // 2. Reject Ghost Record: Artikel wajib memiliki minimal salah satu dari (Penulis, Abstrak, atau DOI)
        boolean hasAuthors = article.authors() != null && !article.authors().isBlank();
        boolean hasAbstract = article.abstractText() != null && !article.abstractText().isBlank();
        boolean hasDoi = article.doi() != null && !article.doi().isBlank();
        if (!hasAuthors && !hasAbstract && !hasDoi) {
            return false;
        }

        // 3. Validasi Rentang Tahun
        if (fromYear != null || toYear != null) {
            if (article.year() == null || article.year().isBlank()) return false;
            try {
                int y = Integer.parseInt(article.year());
                if (fromYear != null && y < fromYear) return false;
                if (toYear != null && y > toYear) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 4. Pencocokan Kata Kunci (Mendukung varian multi-kata kunci OR via koma atau operator OR, dan token AND dalam grup)
        if (keyword != null && !keyword.isBlank()) {
            if (!matchesKeyword(title, article.abstractText(), article.keywords(), keyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Mengevaluasi apakah judul atau abstrak memenuhi kueri kata kunci.
     * Mendukung:
     * - Multi-kata kunci alternatif (OR) menggunakan pemisah koma (,) atau operator 'OR' / 'or'.
     * - Token AND / frasa utuh untuk kata majemuk di dalam masing-masing grup alternatif.
     * - Backward-compatible penuh dengan kueri kata tunggal maupun majemuk sebelumnya.
     */
    public static boolean matchesKeyword(String title, String abstractText, String keywordQuery) {
        return matchesKeyword(title, abstractText, "", keywordQuery);
    }

    public static boolean matchesKeyword(String title, String abstractText, String keywords, String keywordQuery) {
        if (keywordQuery == null || keywordQuery.isBlank()) return true;

        String t = title == null ? "" : title;
        String a = abstractText == null ? "" : abstractText;
        String k = keywords == null ? "" : keywords;
        String haystack = (t + " " + a + " " + k).toLowerCase(Locale.ROOT);

        // Pisahkan kueri ke dalam grup-grup OR (dipisahkan oleh koma ',' atau operator 'OR')
        String[] orGroups = keywordQuery.split("(?i)\\s*,\\s*|\\s+\\bor\\b\\s+");
        for (String group : orGroups) {
            String cleanGroup = group.strip().toLowerCase(Locale.ROOT);
            if (cleanGroup.isEmpty()) continue;

            // Hapus pembungkus tanda kutip jika ada
            if ((cleanGroup.startsWith("\"") && cleanGroup.endsWith("\"") && cleanGroup.length() > 1)
                    || (cleanGroup.startsWith("'") && cleanGroup.endsWith("'") && cleanGroup.length() > 1)) {
                cleanGroup = cleanGroup.substring(1, cleanGroup.length() - 1).strip();
            }
            if (cleanGroup.isEmpty()) continue;

            // Jika frasa utuh persis ditemukan dalam haystack
            if (haystack.contains(cleanGroup)) {
                return true;
            }

            // Jika terdiri dari beberapa kata, seluruh token kata dalam grup harus ada (AND dalam grup)
            String[] tokens = cleanGroup.split("\\s+");
            if (tokens.length > 1) {
                boolean allTokensPresent = Arrays.stream(tokens)
                        .filter(s -> !s.isBlank())
                        .allMatch(haystack::contains);
                if (allTokensPresent) {
                    return true;
                }
            }
        }

        return false;
    }
}
