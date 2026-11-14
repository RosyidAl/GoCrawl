package id.gocrawl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static id.gocrawl.TestSupport.*;

public class CitationExporterTest {

    public static void run() throws IOException {
        System.out.println("\n[TEST SUITE] CitationExporterTest");

        Article article = new Article(
                "Penerapan Kecerdasan Buatan dalam Pendidikan",
                "Ahmad Rosyid; Budi Santoso",
                "2023",
                "Jurnal Pendidikan Modern",
                "https://ejournal.example.org/article/view/100",
                "Studi ini mengkaji efektivitas AI tutor.",
                "10.1234/jpm.2023.100",
                "2023-08-20",
                "ai, pendidikan, kurikulum"
        );

        // 1. Uji Konversi BibTeX
        String bib = CitationExporter.toBibTeX(List.of(article));
        assertTrue("BibTeX harus mengandung @article", bib.contains("@article{"));
        assertTrue("BibTeX harus memformat author dengan 'and'", bib.contains("Ahmad Rosyid and Budi Santoso"));
        assertTrue("BibTeX harus mengandung title", bib.contains("title = {Penerapan Kecerdasan Buatan dalam Pendidikan}"));
        assertTrue("BibTeX harus mengandung journal", bib.contains("journal = {Jurnal Pendidikan Modern}"));
        assertTrue("BibTeX harus mengandung year", bib.contains("year = {2023}"));
        assertTrue("BibTeX harus mengandung doi", bib.contains("doi = {10.1234/jpm.2023.100}"));

        // 2. Uji Konversi RIS
        String ris = CitationExporter.toRIS(List.of(article));
        assertTrue("RIS harus diawali dengan TY  - JOUR", ris.contains("TY  - JOUR"));
        assertTrue("RIS harus memiliki tag TI", ris.contains("TI  - Penerapan Kecerdasan Buatan dalam Pendidikan"));
        assertTrue("RIS harus memisahkan author dalam baris AU terpisah", ris.contains("AU  - Ahmad Rosyid\nAU  - Budi Santoso"));
        assertTrue("RIS harus memiliki tag JO", ris.contains("JO  - Jurnal Pendidikan Modern"));
        assertTrue("RIS harus memiliki tag PY", ris.contains("PY  - 2023"));
        assertTrue("RIS harus diakhiri dengan ER  -", ris.contains("ER  -"));

        // 3. Uji Ekspor Berkas Fisik
        Path tempBib = Files.createTempFile("test_", ".bib");
        Path tempRis = Files.createTempFile("test_", ".ris");
        try {
            CitationExporter.exportBibTeX(tempBib, List.of(article));
            assertTrue("File .bib harus ada", Files.exists(tempBib));
            assertTrue("Isi file .bib tidak boleh kosong", Files.size(tempBib) > 50);

            CitationExporter.exportRIS(tempRis, List.of(article));
            assertTrue("File .ris harus ada", Files.exists(tempRis));
            assertTrue("Isi file .ris tidak boleh kosong", Files.size(tempRis) > 50);
        } finally {
            Files.deleteIfExists(tempBib);
            Files.deleteIfExists(tempRis);
        }
    }
}
