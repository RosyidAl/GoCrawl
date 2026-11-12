package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class GarudaSearchEngineTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] GarudaSearchEngineTest");

        // 1. Uji Ekstraksi Tahun dari Subtitle
        assertEquals("Ekstraksi tahun di dalam kurung", "2024",
                GarudaSearchEngine.extractYear("Jurnal Pengembangan Profesi Guru (JPPG) Vol. 2 No. 1 (2024)"));
        assertEquals("Ekstraksi tahun tanpa kurung", "2023",
                GarudaSearchEngine.extractYear("MATAAZIR Vol. 6 No. 2: Edisi Juli-Desember 2023"));
        assertEquals("Tahun null mengembalikan '-'", "-",
                GarudaSearchEngine.extractYear(null));

        // 2. Uji Ekstraksi Nama Jurnal Bersih
        assertEquals("Ekstraksi nama jurnal sebelum Vol.", "Jurnal Pengembangan Profesi Guru (JPPG)",
                GarudaSearchEngine.extractJournalName("Jurnal Pengembangan Profesi Guru (JPPG) Vol. 2 No. 1 (2024)"));
        assertEquals("Ekstraksi nama jurnal sebelum Edisi", "MATAAZIR",
                GarudaSearchEngine.extractJournalName("MATAAZIR Vol. 6 No. 2: Edisi 2023"));

        // 2b. Uji Pembersihan Judul Artikel (Rogue prefixes & duplicate repetitions)
        assertEquals("Bersihkan prefix huruf tunggal rogue 'a '", "Peran Supervisi Pendidikan",
                GarudaSearchEngine.cleanGarudaTitle("a Peran Supervisi Pendidikan"));
        assertEquals("Bersihkan repetisi judul identik terpisah kolon", "Keterkaitan Antara Politik Pendidikan",
                GarudaSearchEngine.cleanGarudaTitle("Keterkaitan Antara Politik Pendidikan: Keterkaitan Antara Politik Pendidikan"));
        assertEquals("Bersihkan frase berulang dengan pemotongan karakter batas database", "PENDIDIKAN KARAKTER ANAK USIA SEKOLAH DASAR",
                GarudaSearchEngine.cleanGarudaTitle("PENDIDIKAN KARAKTER ANAK USIA SEKOLAH DASAR PENDIDIKAN KARAKTER ANAK USIA SEKOLAH DASAR PENDIDIKAN KARAKTER ANAK USIA SEKOL"));

        // 3. Uji Parsing HTML Hasil Pencarian Portal Garuda
        String sampleHtml = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p class="pagination-info">
                        Page 1 of 50 | Total Record : 500
                    </p>
                    <div class="article-item">
                        <a class="title-article" href="/documents/detail/4107696">
                            <xmp>Keterkaitan Antara Politik Pendidikan dan Kebijakan</xmp>
                        </a>
                        <a class="author-article" href="/author/1"><xmp>Damanhuri</xmp></a>;
                        <a class="author-article" href="/author/2"><xmp>Shodikin</xmp></a>
                        <br>
                        <xmp class="subtitle-article"> Jurnal Pengembangan Profesi Guru (JPPG) Vol. 2 No. 1 (2024) </xmp>
                        <br>
                        <i class="subtitle-article">Publisher : </i><xmp class="subtitle-article">LPPM STAI Darussalam</xmp>
                        <p class="action-article">
                            <a class="btn_show_abstract title-citation" target="4107696">Show Abstract</a>
                            | <a class="title-citation" href="https://ejournal.test.ac.id/article/download/648/488">
                                Download Original
                              </a>
                            | <i>
                                <a class="title-citation" href="https://ejournal.test.ac.id/article/view/648/488">
                                    Original Source
                                </a>
                              </i>
                            | <a class="title-citation" href="https://doi.org/10.52266/test.v8i1.2777">
                                DOI: 10.52266/test.v8i1.2777
                              </a>
                        </p>
                        <div id="show_abstract4107696" class="article-abstract">
                            <div class="abstract-article">
                                <xmp class="abstract-article">Ini adalah teks abstrak artikel ilmiah pengujian.</xmp>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        JournalCatalog catalog = new JournalCatalog();
        catalog.register(new Journal(
                "garuda-4107",
                "Jurnal Pengembangan Profesi Guru (JPPG)",
                "LPPM STAI Darussalam",
                "4107",
                "97",
                "Education",
                "S2",
                "https://ejournal.test.ac.id/jppg",
                "",
                "1111-2222",
                "3333-4444",
                true
        ));

        GarudaSearchEngine engine = new GarudaSearchEngine();
        GarudaSearchEngine.SearchOptions opts = new GarudaSearchEngine.SearchOptions("politik pendidikan", "ALL", "ALL", null, null, 1);
        GarudaSearchEngine.PageResult result = engine.parseDocumentPage(sampleHtml, opts, catalog);

        assertEquals("Current page sesuai", 1, result.currentPage());
        assertEquals("Total pages sesuai", 50, result.totalPages());
        assertEquals("Total records sesuai", 500, result.totalRecords());
        assertTrue("Has more pages", result.hasMore());
        assertEquals("Jumlah artikel berhasil diparse", 1, result.articles().size());

        Article a = result.articles().get(0);
        assertEquals("Judul artikel cocok", "Keterkaitan Antara Politik Pendidikan dan Kebijakan", a.title());
        assertEquals("Penulis artikel cocok", "Damanhuri; Shodikin", a.authors());
        assertEquals("Tahun artikel cocok", "2024", a.year());
        assertEquals("Nama jurnal cocok", "Jurnal Pengembangan Profesi Guru (JPPG)", a.journal());
        assertEquals("Tautan OJS asli cocok", "https://ejournal.test.ac.id/article/view/648/488", a.url());
        assertEquals("DOI cocok", "10.52266/test.v8i1.2777", a.doi());
        assertEquals("Abstrak cocok", "Ini adalah teks abstrak artikel ilmiah pengujian.", a.abstractText());

        // 4. Uji Filter SINTA saat parsing
        GarudaSearchEngine.SearchOptions sintaOptPass = new GarudaSearchEngine.SearchOptions("politik", "ALL", "S2", null, null, 1);
        GarudaSearchEngine.PageResult passResult = engine.parseDocumentPage(sampleHtml, sintaOptPass, catalog);
        assertEquals("Jurnal S2 lolos minimal S2", 1, passResult.articles().size());

        GarudaSearchEngine.SearchOptions sintaOptFail = new GarudaSearchEngine.SearchOptions("politik", "ALL", "S1", null, null, 1);
        GarudaSearchEngine.PageResult failResult = engine.parseDocumentPage(sampleHtml, sintaOptFail, catalog);
        assertEquals("Jurnal S2 tidak lolos minimal S1", 0, failResult.articles().size());

        // 5. Uji Filter Bidang Ilmu saat parsing
        GarudaSearchEngine.SearchOptions fieldPass = new GarudaSearchEngine.SearchOptions("politik", "97", "ALL", null, null, 1);
        GarudaSearchEngine.PageResult fieldPassRes = engine.parseDocumentPage(sampleHtml, fieldPass, catalog);
        assertEquals("Jurnal Education (97) lolos filter bidang 97", 1, fieldPassRes.articles().size());

        GarudaSearchEngine.SearchOptions fieldFail = new GarudaSearchEngine.SearchOptions("politik", "60", "ALL", null, null, 1);
        GarudaSearchEngine.PageResult fieldFailRes = engine.parseDocumentPage(sampleHtml, fieldFail, catalog);
        assertEquals("Jurnal Education (97) tidak lolos filter bidang 60", 0, fieldFailRes.articles().size());
    }
}
