package id.gocrawl;

/**
 * Model data metadata artikel ilmiah hasil ekstraksi.
 */
public record Article(
        String title,
        String authors,
        String year,
        String journal,
        String url,
        String abstractText,
        String doi,
        String published,
        String keywords
) {}
