package id.gocrawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ekstraktor metadata kaskade berbasis Jsoup:
 * Memprioritaskan tag sitasi akademik Highwire Press & Dublin Core,
 * dengan fallback ke penyeleksi DOM CSS OJS modern.
 */
public final class MetadataExtractor {
    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    private MetadataExtractor() {}

    public static Article extractArticle(String html, URI requestUri, String fallbackJournalName) {
        Document doc = Jsoup.parse(html, requestUri.toString());
        Map<String, List<String>> meta = extractMetaTags(doc);

        // 1. Ekstraksi Judul
        String title = firstMeta(meta, "citation_title", "dc.title", "dc.title.alternative", "og:title");
        if (title.isBlank()) {
            Element el = doc.selectFirst("h1.page_title, .page_article h1, h1.entry-title, h1.article-title, .item.title, .article-details-title");
            if (el != null) title = el.text();
        }
        if (title.isBlank()) {
            title = doc.title();
        }
        title = TextSanitizer.cleanTitle(title);

        // 2. Ekstraksi Penulis
        List<String> authorsList = allMeta(meta, "citation_author", "dc.creator", "dc.contributor.author", "author");
        if (authorsList.isEmpty()) {
            Elements authorEls = doc.select("ul.authors li .name, .author-name, .authors .name, .item.authors .value, .entry-author");
            for (Element a : authorEls) {
                String name = a.text().strip();
                if (!name.isBlank() && !authorsList.contains(name)) {
                    authorsList.add(name);
                }
            }
        }
        String authors = String.join("; ", authorsList.stream().map(TextSanitizer::clean).filter(s -> !s.isBlank()).toList());

        // 3. Ekstraksi Tanggal Publikasi (Published)
        String published = firstMeta(meta,
                "citation_date",
                "citation_publication_date",
                "citation_cover_date",
                "citation_online_date",
                "dc.date.issued",
                "dc.date",
                "dc.date.created",
                "prism.publicationdate");
        if (published.isBlank()) {
            Element el = doc.selectFirst(".item.published .value, .published .value, .item.date .value, .entry-details .published, .article-date");
            if (el != null) published = el.text();
        }
        published = TextSanitizer.clean(published);

        // 4. Ekstraksi Tahun
        String year = extractYear(published);
        if (year.isBlank()) {
            year = extractYear(firstMeta(meta, "citation_year", "citation_date", "dc.date"));
        }

        // 5. Ekstraksi Abstrak
        String abstractText = firstMeta(meta, "citation_abstract", "dc.description", "dc.description.abstract", "og:description");
        if (abstractText.isBlank()) {
            Element el = doc.selectFirst(".item.abstract .value, section.item.abstract p, .article-abstract, .abstract-content, .item.abstract, .entry-abstract");
            if (el != null) abstractText = el.text();
        }
        abstractText = TextSanitizer.clean(abstractText);

        // 6. Ekstraksi DOI
        String doi = firstMeta(meta, "citation_doi", "dc.identifier.doi", "prism.doi");
        if (doi.isBlank()) {
            for (String idVal : allMeta(meta, "dc.identifier")) {
                if (idVal.contains("10.") && idVal.contains("/")) {
                    doi = idVal;
                    break;
                }
            }
        }
        if (doi.isBlank()) {
            Element el = doc.selectFirst(".item.doi .value a, .item.doi a, .doi a");
            if (el != null) {
                doi = el.text().isBlank() ? el.attr("href") : el.text();
            }
        }
        doi = TextSanitizer.cleanDoi(doi);

        // 7. Ekstraksi Nama Jurnal
        String journal = firstMeta(meta, "citation_journal_title", "citation_journal_abbrev", "dc.source", "dc.relation.ispartof");
        if (journal.isBlank()) {
            Element el = doc.selectFirst(".journal-title, .navbar-brand, .site-name");
            if (el != null) journal = el.text();
        }
        if (journal.isBlank() && doc.title().contains("|")) {
            String suffix = doc.title().substring(doc.title().lastIndexOf('|') + 1).strip();
            if (!suffix.isBlank()) journal = suffix;
        }
        if (journal.isBlank()) {
            journal = fallbackJournalName;
        }
        journal = TextSanitizer.clean(journal);

        // 8. Ekstraksi URL Kanonikal
        String canonicalUrl = firstMeta(meta, "citation_public_url");
        if (canonicalUrl.isBlank() || !canonicalUrl.contains("/article/view/")) {
            canonicalUrl = UrlFilter.canonicalizeArticleUrl(requestUri);
        }

        // 9. Ekstraksi Kata Kunci (Keywords)
        List<String> kwList = allMeta(meta, "citation_keywords", "dc.subject", "dc.subject.keyword", "keywords");
        String keywords = "";
        if (!kwList.isEmpty()) {
            keywords = String.join(", ", kwList);
        } else {
            Element el = doc.selectFirst(".item.keywords .value, .article-keywords .value, .keywords .value, section.item.keywords .value, .sub_item.keywords .value");
            if (el != null) {
                keywords = el.text();
            }
        }
        if (keywords.isBlank() && !abstractText.isBlank()) {
            Matcher m = Pattern.compile("(?i)(?:kata\\s*kunci|keywords?)\\s*:\\s*(.+)$").matcher(abstractText);
            if (m.find()) {
                keywords = m.group(1).replaceAll("[.\"]+$", "").strip();
            }
        }
        keywords = TextSanitizer.clean(keywords);

        return new Article(title, authors, year, journal, canonicalUrl, abstractText, doi, published, keywords);
    }

    public static List<String> extractLinks(String html, URI baseUri) {
        Document doc = Jsoup.parse(html, baseUri.toString());
        Elements elements = doc.select("a[href]");
        List<String> links = new ArrayList<>();
        for (Element el : elements) {
            String href = el.attr("abs:href").strip();
            if (!href.isBlank()) {
                links.add(href);
            }
        }
        return links;
    }

    private static Map<String, List<String>> extractMetaTags(Document doc) {
        Map<String, List<String>> meta = new LinkedHashMap<>();
        for (Element el : doc.select("meta")) {
            String name = el.attr("name").strip();
            if (name.isBlank()) name = el.attr("property").strip();
            if (name.isBlank()) continue;

            String content = el.attr("content").strip();
            if (!content.isBlank()) {
                meta.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(content);
            }
        }
        return meta;
    }

    private static String firstMeta(Map<String, List<String>> meta, String... keys) {
        for (String k : keys) {
            List<String> values = meta.get(k.toLowerCase(Locale.ROOT));
            if (values != null) {
                for (String v : values) {
                    if (v != null && !v.isBlank()) return v.strip();
                }
            }
        }
        return "";
    }

    private static List<String> allMeta(Map<String, List<String>> meta, String... keys) {
        List<String> result = new ArrayList<>();
        for (String k : keys) {
            List<String> values = meta.get(k.toLowerCase(Locale.ROOT));
            if (values != null) {
                for (String v : values) {
                    if (v != null && !v.isBlank()) result.add(v.strip());
                }
            }
        }
        return result;
    }

    private static String extractYear(String val) {
        if (val == null || val.isBlank()) return "";
        Matcher m = YEAR_PATTERN.matcher(val);
        return m.find() ? m.group() : "";
    }
}
