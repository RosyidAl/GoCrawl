package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class QualityGateTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] QualityGateTest");

        Article validArticle = new Article(
                "Penerapan Machine Learning untuk Klasifikasi Teks Ilmiah",
                "Ahmad Rosyid; Budi Santoso",
                "2023",
                "Jurnal Sistem Cerdas",
                "https://ejournal.example.org/article/view/101",
                "Penelitian ini menganalisis algoritma supervised learning pada korpus bahasa Indonesia.",
                "10.1234/jsc.2023.101",
                "2023-05-15",
                "machine learning, text classification, nlp"
        );

        // 1. Artikel Valid
        assertTrue("Artikel lengkap harus lolos Quality Gate",
                QualityGate.isValid(validArticle, "machine learning", 2020, 2024));

        // 2. Reject Judul Kosong / Null
        Article nullTitle = new Article(null, "Penulis", "2023", "Jurnal", "https://url", "Abstrak", "10.1234/x", "2023", "k");
        assertFalse("Judul null harus ditolak",
                QualityGate.isValid(nullTitle, null, null, null));

        Article blankTitle = new Article("   ", "Penulis", "2023", "Jurnal", "https://url", "Abstrak", "10.1234/x", "2023", "k");
        assertFalse("Judul blank harus ditolak",
                QualityGate.isValid(blankTitle, null, null, null));

        // 3. Reject Judul Artefak Galley ("View of", "Tampilan")
        Article viewOf = new Article("View of", "Penulis", "2023", "Jurnal", "https://url", "Abstrak", "10.1234/x", "2023", "k");
        assertFalse("Judul 'View of' harus ditolak",
                QualityGate.isValid(viewOf, null, null, null));

        Article tampilan = new Article("tampilan", "Penulis", "2023", "Jurnal", "https://url", "Abstrak", "10.1234/x", "2023", "k");
        assertFalse("Judul 'tampilan' harus ditolak",
                QualityGate.isValid(tampilan, null, null, null));

        // 4. Reject Ghost Record (tidak ada Penulis, Abstrak, maupun DOI)
        Article ghost = new Article("Sebuah Judul Tanpa Metadata", "", "2023", "Jurnal", "https://url", "", "", "2023", "");
        assertFalse("Ghost record tanpa penulis, abstrak, dan DOI harus ditolak",
                QualityGate.isValid(ghost, null, null, null));

        // 5. Validasi Rentang Tahun
        assertFalse("Tahun 2023 harus ditolak jika fromYear=2024",
                QualityGate.isValid(validArticle, null, 2024, 2025));
        assertFalse("Tahun 2023 harus ditolak jika toYear=2022",
                QualityGate.isValid(validArticle, null, 2020, 2022));
        assertTrue("Tahun 2023 harus lolos pada rentang 2022-2024",
                QualityGate.isValid(validArticle, null, 2022, 2024));

        // 6. Pencocokan Kata Kunci (Multi-keyword OR dan AND)
        assertTrue("Cocok dengan frasa di judul",
                QualityGate.matchesKeyword(validArticle.title(), validArticle.abstractText(), "machine learning"));
        assertTrue("Cocok dengan varian alternatif OR (koma)",
                QualityGate.matchesKeyword(validArticle.title(), validArticle.abstractText(), "deep learning, machine learning"));
        assertTrue("Cocok dengan varian alternatif OR (operator)",
                QualityGate.matchesKeyword(validArticle.title(), validArticle.abstractText(), "robotika OR supervised learning"));
        assertFalse("Tidak cocok dengan kata kunci di luar artikel",
                QualityGate.matchesKeyword(validArticle.title(), validArticle.abstractText(), "astronomi"));
    }
}
