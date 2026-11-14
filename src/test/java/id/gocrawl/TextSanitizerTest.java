package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class TextSanitizerTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] TextSanitizerTest");

        // 1. Pembersihan Entitas HTML
        String htmlText = "Riset &amp; Pengembangan: &quot;Model Pembelajaran&quot; &#39;Abad 21&#39; &nbsp; &rsquo;Karakter&lsquo;";
        String cleaned = TextSanitizer.clean(htmlText);
        assertEquals("Entitas HTML harus diterjemahkan",
                "Riset & Pengembangan: \"Model Pembelajaran\" 'Abad 21' ’Karakter‘", cleaned);

        // 2. Pembersihan Judul dari Artefak Galley ("View of", "Tampilan")
        String titleViewOf = "View of Penerapan Kurikulum Merdeka di Sekolah Dasar";
        assertEquals("Prefix 'View of' harus dibersihkan",
                "Penerapan Kurikulum Merdeka di Sekolah Dasar", TextSanitizer.cleanTitle(titleViewOf));

        String titleTampilan = "Tampilan Implementasi Algoritma Genetika";
        assertEquals("Prefix 'Tampilan' harus dibersihkan",
                "Implementasi Algoritma Genetika", TextSanitizer.cleanTitle(titleTampilan));

        // 3. Pembersihan Sufiks Pipe ("| Nama Jurnal")
        String titleWithJournal = "Analisis Sentimen Twitter | Jurnal Sistem Informasi";
        assertEquals("Sufiks nama jurnal setelah pipe harus dibersihkan",
                "Analisis Sentimen Twitter", TextSanitizer.cleanTitle(titleWithJournal));

        // 4. Normalisasi DOI
        String doiUrl = "https://doi.org/10.1234/jsi.v5i2.105";
        assertEquals("URL DOI harus dinormalisasi menjadi kode DOI bersih",
                "10.1234/jsi.v5i2.105", TextSanitizer.cleanDoi(doiUrl));

        String doiPrefix = "doi: 10.5678/pedagogik.2023.45";
        assertEquals("Prefix 'doi:' harus dihapus",
                "10.5678/pedagogik.2023.45", TextSanitizer.cleanDoi(doiPrefix));

        String doiGalley = "10.1234/jsi.v5i2.105.g3452";
        assertEquals("Sufiks galley .gXXXX harus dihapus",
                "10.1234/jsi.v5i2.105", TextSanitizer.cleanDoi(doiGalley));

        String doiPdf = "10.1234/jsi.v5i2.105/pdf";
        assertEquals("Sufiks /pdf harus dihapus",
                "10.1234/jsi.v5i2.105", TextSanitizer.cleanDoi(doiPdf));
    }
}
