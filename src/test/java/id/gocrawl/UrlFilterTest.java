package id.gocrawl;

import java.net.URI;

import static id.gocrawl.TestSupport.*;

public class UrlFilterTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] UrlFilterTest");

        // 1. URL Landing Page Artikel yang Valid
        URI validArticle1 = URI.create("https://ejournal.upi.edu/index.php/asset/article/view/12693");
        assertTrue("Landing page OJS 2/3 harus valid", UrlFilter.isArticleLandingUrl(validArticle1));

        URI validArticle2 = URI.create("https://jurnal.uns.ac.id/index.php/teknika/article/view/54321/");
        assertTrue("Landing page dengan trailing slash harus valid", UrlFilter.isArticleLandingUrl(validArticle2));

        // 2. Galley Viewer (Viewer PDF/HTML di OJS) -> Harus Ditolak
        URI galleyPdf = URI.create("https://ejournal.upi.edu/index.php/asset/article/view/12693/4504");
        assertFalse("Galley viewer PDF harus ditolak", UrlFilter.isArticleLandingUrl(galleyPdf));

        URI galleyHtml = URI.create("https://ejournal.upi.edu/index.php/asset/article/view/12693/pdf_102");
        assertFalse("Galley viewer dengan nama pdf_xxx harus ditolak", UrlFilter.isArticleLandingUrl(galleyHtml));

        // 3. Download links -> Harus Ditolak
        URI downloadLink = URI.create("https://ejournal.upi.edu/index.php/asset/article/download/12693/4504");
        assertFalse("Tautan unduh berkas langsung harus ditolak", UrlFilter.isArticleLandingUrl(downloadLink));

        // 4. Issue archive / non-article pages -> Harus Ditolak
        URI issuePage = URI.create("https://ejournal.upi.edu/index.php/asset/issue/view/512");
        assertFalse("Halaman issue bukan article landing url", UrlFilter.isArticleLandingUrl(issuePage));

        // 5. Ekstraksi ID Artikel
        String extractedId = UrlFilter.extractArticleId(validArticle1);
        assertEquals("Ekstraksi articleId dari URL", "12693", extractedId);
    }
}
