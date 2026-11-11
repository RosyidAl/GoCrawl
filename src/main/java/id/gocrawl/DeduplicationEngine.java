package id.gocrawl;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Engine deduplikasi 3 lapis (URL/ID, DOI ternormalisasi, dan Sidik Jari Konten Semantik)
 * untuk mencegah duplikasi artikel antar edisi atau akibat variasi URL galley.
 */
public final class DeduplicationEngine {
    private final Set<String> seenUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> seenArticleIds = ConcurrentHashMap.newKeySet();
    private final Set<String> seenDois = ConcurrentHashMap.newKeySet();
    private final Set<String> seenFingerprints = ConcurrentHashMap.newKeySet();

    /**
     * Memeriksa apakah artikel merupakan duplikat dari data yang sudah pernah dicrawl.
     * Jika bukan duplikat, seluruh identitas artikel didaftarkan secara atomik.
     *
     * @return true jika duplikat, false jika artikel baru (unik).
     */
    public synchronized boolean isDuplicateAndRegister(
            String canonicalUrl,
            String articleId,
            String rawDoi,
            String rawTitle,
            String rawAuthors,
            String year
    ) {
        String normUrl = canonicalUrl == null ? "" : canonicalUrl.strip().toLowerCase(Locale.ROOT);
        String normId = articleId == null ? "" : articleId.strip().toLowerCase(Locale.ROOT);
        String normDoi = TextSanitizer.cleanDoi(rawDoi).toLowerCase(Locale.ROOT);

        String cleanTitle = TextSanitizer.cleanTitle(rawTitle);
        String normTitle = cleanTitle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String normAuthor = extractFirstAuthorSurname(rawAuthors).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String normYear = year == null ? "" : year.strip();
        String fingerprint = normTitle + "|" + normAuthor + "|" + normYear;

        // Lapis 1: URL Kanonikal & Article ID
        if (!normUrl.isEmpty() && seenUrls.contains(normUrl)) return true;
        if (!normId.isEmpty() && seenArticleIds.contains(normId)) return true;

        // Lapis 2: Normalized DOI (sudah dibersihkan dari suffix .gXXXX)
        if (!normDoi.isEmpty() && seenDois.contains(normDoi)) return true;

        // Lapis 3: Sidik Jari Konten Semantik (Judul + Penulis Pertama + Tahun)
        if (!normTitle.isEmpty() && seenFingerprints.contains(fingerprint)) return true;

        // Bukan duplikat -> Registrasikan seluruh identitas
        if (!normUrl.isEmpty()) seenUrls.add(normUrl);
        if (!normId.isEmpty()) seenArticleIds.add(normId);
        if (!normDoi.isEmpty()) seenDois.add(normDoi);
        if (!normTitle.isEmpty()) seenFingerprints.add(fingerprint);

        return false;
    }

    private static String extractFirstAuthorSurname(String authors) {
        if (authors == null || authors.isBlank()) return "";
        String first = authors.split(";")[0].strip();
        String[] parts = first.split("\\s+");
        return parts[parts.length - 1].strip();
    }
}
