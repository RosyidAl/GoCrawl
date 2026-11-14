package id.gocrawl;

/**
 * Satu sumber crawl: portal tempat pencarian artikel dilakukan.
 * Sumber bawaan (Garuda) tidak dapat dihapus dari pengelola sumber.
 */
public record CrawlSource(String name, String baseUrl, boolean enabled, boolean builtin) {
}
