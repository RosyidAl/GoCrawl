package id.gocrawl;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Validator dan normalisator pola URL OJS untuk menyaring galley/viewer link
 * dan memastikan hanya landing page artikel kanonikal yang diindeks.
 */
public final class UrlFilter {
    private static final String ARTICLE_VIEW_PATH = "/article/view/";
    private static final Set<String> VALID_SCHEMES = Set.of("http", "https");

    private UrlFilter() {}

    /**
     * Memeriksa apakah suatu URI mengarah ke landing page metadata artikel kanonikal,
     * bukan halaman galley viewer (PDF/HTML viewer) atau direct file download.
     */
    public static boolean isArticleLandingUrl(URI uri) {
        if (uri == null || uri.getPath() == null) return false;
        if (!VALID_SCHEMES.contains(uri.getScheme())) return false;

        String path = uri.getPath();
        int idx = path.toLowerCase(Locale.ROOT).indexOf(ARTICLE_VIEW_PATH);
        if (idx < 0) return false;

        String remainder = path.substring(idx + ARTICLE_VIEW_PATH.length());
        while (remainder.endsWith("/")) {
            remainder = remainder.substring(0, remainder.length() - 1);
        }
        if (remainder.isBlank()) return false;

        // Galley viewer pada OJS memiliki segmen tambahan setelah articleId,
        // misal: /article/view/12693/4504 atau /article/view/21442/pdf_106
        if (remainder.contains("/")) return false;

        // Blacklist tautan non-artikel
        String lower = remainder.toLowerCase(Locale.ROOT);
        if (lower.startsWith("download") || lower.startsWith("viewfile") || lower.contains("pdf")) {
            return false;
        }

        return true;
    }

    /**
     * Mengekstrak ID artikel kanonikal dari path URL OJS.
     * Contoh: /index.php/jusan/article/view/12693 -> "12693"
     */
    public static String extractArticleId(URI uri) {
        if (uri == null || uri.getPath() == null) return "";
        String path = uri.getPath();
        int idx = path.toLowerCase(Locale.ROOT).indexOf(ARTICLE_VIEW_PATH);
        if (idx < 0) return "";
        String remainder = path.substring(idx + ARTICLE_VIEW_PATH.length());
        int slash = remainder.indexOf('/');
        if (slash >= 0) remainder = remainder.substring(0, slash);
        return remainder.strip();
    }

    /**
     * Menghasilkan URL kanonikal bersih tanpa query parameter, hash, atau trailing slash.
     */
    public static String canonicalizeArticleUrl(URI uri) {
        if (uri == null) return "";
        String id = extractArticleId(uri);
        if (id.isBlank()) return uri.toString();

        String path = uri.getPath();
        int idx = path.toLowerCase(Locale.ROOT).indexOf(ARTICLE_VIEW_PATH);
        if (idx < 0) return uri.toString();

        String basePath = path.substring(0, idx + ARTICLE_VIEW_PATH.length()) + id;
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
            sb.append(":").append(uri.getPort());
        }
        sb.append(basePath);
        return sb.toString();
    }
}
