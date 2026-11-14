package id.gocrawl;

/**
 * Satu sumber crawl: portal tempat pencarian artikel dilakukan.
 * Tipe "garuda" memakai parser Portal Garuda; tipe "ojs" memakai crawler
 * edisi (arsip → edisi → artikel) untuk portal Open Journal Systems.
 */
public record CrawlSource(String name, String baseUrl, String type, boolean enabled, boolean builtin) {

    public static final String TYPE_GARUDA = "garuda";
    public static final String TYPE_OJS = "ojs";
}
