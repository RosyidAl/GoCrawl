package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class DeduplicationEngineTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] DeduplicationEngineTest");

        DeduplicationEngine dedup = new DeduplicationEngine();

        // 1. Artikel Pertama (Unik -> harus return false/bukan duplikat)
        boolean dup1 = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/100",
                "100",
                "10.1234/asset.v1i1.100",
                "Analisis Kinerja Keuangan",
                "Bambang Sudarmono; Joko Widodo",
                "2023"
        );
        assertFalse("Artikel unik pertama tidak boleh dianggap duplikat", dup1);

        // 2. Lapis 1: Duplikasi URL Kanonikal
        boolean dupUrl = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/100",
                "999",
                "10.9999/beda",
                "Judul Berbeda",
                "Penulis Berbeda",
                "2024"
        );
        assertTrue("URL kanonikal yang sama harus terdeteksi sebagai duplikat", dupUrl);

        // 3. Lapis 1: Duplikasi Article ID
        boolean dupId = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/diff",
                "100",
                "10.9999/beda",
                "Judul Berbeda",
                "Penulis Berbeda",
                "2024"
        );
        assertTrue("Article ID yang sama harus terdeteksi sebagai duplikat", dupId);

        // 4. Lapis 2: Duplikasi DOI Ternormalisasi (menangani suffix .gXXXX)
        boolean dupDoi = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/200",
                "200",
                "10.1234/asset.v1i1.100.g450", // suffix galley
                "Judul Berbeda",
                "Penulis Berbeda",
                "2024"
        );
        assertTrue("DOI dengan sufiks galley .gXXXX harus terdeteksi duplikat terhadap DOI induk", dupDoi);

        // 5. Lapis 3: Sidik Jari Konten Semantik (Judul + Penulis Utama + Tahun)
        boolean dupFingerprint = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/300",
                "300",
                "",
                "Analisis Kinerja Keuangan",
                "Bambang Sudarmono",
                "2023"
        );
        assertTrue("Sidik jari semantik (Judul + Penulis + Tahun) yang sama harus terdeteksi sebagai duplikat", dupFingerprint);

        // 6. Entitas Benar-benar Baru
        boolean dupNew = dedup.isDuplicateAndRegister(
                "https://ejournal.upi.edu/index.php/asset/article/view/500",
                "500",
                "10.1234/asset.v1i1.500",
                "Pengaruh Motivasi Terhadap Produktivitas",
                "Siti Rahayu",
                "2024"
        );
        assertFalse("Entitas benar-benar baru tidak boleh dianggap duplikat", dupNew);
    }
}
