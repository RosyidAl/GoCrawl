package id.gocrawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Crawler untuk portal Open Journal Systems (OJS 3): menjelajah arsip edisi,
 * membedah artikel tiap edisi (struktur .obj_article_summary yang seragam antar
 * jurnal OJS), lalu memperkaya metadata dari halaman artikel (citation_title,
 * citation_author, citation_doi, citation_publication_date, abstrak).
 */
public class OjsCrawler {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final String baseUrl;

    public OjsCrawler(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                .timeout(20000)
                .get();
    }

    /** Mengumpulkan URL seluruh edisi dari halaman arsip (urutan terbaru dulu). */
    public List<String> listIssues() {
        List<String> issues = new ArrayList<>();
        try {
            Document doc = fetch(baseUrl + "/issue/archive");
            for (Element a : doc.select("a[href]")) {
                String href = a.attr("abs:href");
                if (href.contains("/issue/view/") && !issues.contains(href)) {
                    issues.add(href);
                }
            }
        } catch (Exception ignored) {
            // arsip tidak tersedia — daftar kosong
        }
        return issues;
    }

    /**
     * Membedah satu halaman edisi menjadi artikel parsial
     * (judul, penulis, URL — tahun/DOI/abstrak dilengkapi lewat enrich()).
     */
    public List<Article> parseIssue(String issueUrl) {
        List<Article> articles = new ArrayList<>();
        try {
            Document doc = fetch(issueUrl);
            for (Element summary : doc.select(".obj_article_summary")) {
                Element titleLink = summary.selectFirst("a[href*=/article/view/]");
                if (titleLink == null) continue;
                String url = titleLink.attr("abs:href");
                // Buang galley suffix (…/article/view/1090/568 → …/article/view/1090)
                url = url.replaceAll("(/article/view/\\d+)/\\d+", "$1");

                String title = titleLink.text().strip();
                if (title.isBlank()) continue;

                String authors = "-";
                Element authorsEl = summary.selectFirst(".authors");
                if (authorsEl != null && !authorsEl.text().isBlank()) {
                    authors = authorsEl.text().strip();
                }

                articles.add(new Article(title, authors, "-", "-", url, "", "-", "", ""));
            }
        } catch (Exception ignored) {
            // edisi gagal dibaca — lewati
        }
        return articles;
    }

    /**
     * Memperkaya artikel parsial dengan metadata lengkap dari halamannya
     * (DOI, tahun, penulis kanonik, abstrak, kata kunci).
     * Tanggal: OJS 3 memakai citation_date (bukan citation_publication_date).
     */
    public Article enrich(Article partial) {
        try {
            Document doc = fetch(partial.url());
            String title = meta(doc, "citation_title");
            String doi = meta(doc, "citation_doi");
            String pubDate = meta(doc, "citation_publication_date");
            if (pubDate.isEmpty()) pubDate = meta(doc, "citation_date");
            if (pubDate.isEmpty()) pubDate = meta(doc, "citation_online_date");
            String journal = meta(doc, "citation_journal_title");
            String keywords = meta(doc, "citation_keywords");

            List<String> authors = new ArrayList<>();
            for (Element el : doc.select("meta[name=citation_author]")) {
                String a = el.attr("content").strip();
                if (!a.isEmpty()) authors.add(a);
            }

            String abstractText = "";
            Element absEl = doc.selectFirst("section.item.abstract");
            if (absEl == null) absEl = doc.selectFirst("section.abstract");
            if (absEl != null) {
                abstractText = TextSanitizer.clean(absEl.text().replaceFirst("(?i)^abstract\\s*", "").strip());
            }

            String year = pubDate.length() >= 4 ? pubDate.substring(0, 4) : partial.year();
            String finalTitle = title.isEmpty() ? partial.title() : title;
            String finalAuthors = authors.isEmpty() ? partial.authors() : String.join("; ", authors);
            String finalJournal = journal.isEmpty() ? partial.journal() : journal;
            String finalDoi = doi.isEmpty() ? partial.doi() : doi;
            String published = pubDate.isEmpty() ? partial.published() : pubDate;
            String finalKeywords = keywords.isEmpty() ? partial.keywords() : keywords;

            return new Article(finalTitle, finalAuthors, year, finalJournal, partial.url(),
                    abstractText, finalDoi, published, finalKeywords);
        } catch (Exception ex) {
            return partial; // metadata gagal diambil — pakai data parsial dari edisi
        }
    }

    private static String meta(Document doc, String name) {
        Element el = doc.selectFirst("meta[name=" + name + "]");
        return el == null ? "" : el.attr("content").strip();
    }
}
