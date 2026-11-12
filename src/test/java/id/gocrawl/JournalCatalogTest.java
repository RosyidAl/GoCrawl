package id.gocrawl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static id.gocrawl.TestSupport.*;

public class JournalCatalogTest {

    public static void run() throws IOException {
        System.out.println("\n[TEST SUITE] JournalCatalogTest");

        Journal j1 = new Journal(
                "garuda-6001",
                "Jurnal Ilmu Komputer",
                "Universitas Indonesia",
                "6001",
                "60",
                "Computer Science & IT",
                "S2",
                "https://cs.ui.ac.id/journal",
                "https://cs.ui.ac.id/journal/oai",
                "1234-5678",
                "8765-4321",
                true
        );

        Journal j2 = new Journal(
                "garuda-9701",
                "Jurnal Pedagogik Modern",
                "UPI Bandung",
                "9701",
                "97",
                "Education",
                "S1",
                "https://upi.edu/pedagogik",
                "https://upi.edu/pedagogik/oai",
                "2345-6789",
                "9876-5432",
                true
        );

        // 1. Registrasi Jurnal
        JournalCatalog catalog = new JournalCatalog();
        catalog.register(j1);
        catalog.register(j2);
        assertEquals("Ukuran katalog harus 2", 2, catalog.size());

        // 2. Registrasi duplikat garudaId harus menimpa/update
        Journal j1Updated = new Journal(
                "garuda-6001",
                "Jurnal Ilmu Komputer Updated",
                "Universitas Indonesia",
                "6001",
                "60",
                "Computer Science & IT",
                "S1",
                "https://cs.ui.ac.id/journal",
                "https://cs.ui.ac.id/journal/oai",
                "1234-5678",
                "8765-4321",
                true
        );
        catalog.register(j1Updated);
        assertEquals("Ukuran katalog tetap 2 setelah update entri sama", 2, catalog.size());

        // 3. Filter berdasarkan Subjek ID
        List<Journal> csList = catalog.filterBySubject("60");
        assertEquals("Filter subjek 60 harus menghasilkan 1 jurnal", 1, csList.size());
        assertEquals("Nama jurnal hasil filter harus terupdate", "Jurnal Ilmu Komputer Updated", csList.get(0).name());

        // 4. Filter berdasarkan Nama Subjek dan Alias Bahasa Indonesia
        List<Journal> eduList = catalog.filterBySubject("Education");
        assertEquals("Filter subjek Education harus menghasilkan 1 jurnal", 1, eduList.size());
        assertEquals("ID jurnal hasil filter", "9701", eduList.get(0).garudaId());

        List<Journal> eduAliasList = catalog.filterBySubject("pendidikan");
        assertEquals("Filter alias 'pendidikan' harus menghasilkan 1 jurnal", 1, eduAliasList.size());
        assertEquals("ID jurnal hasil filter alias", "9701", eduAliasList.get(0).garudaId());

        List<Journal> allList = catalog.filterBySubject("ALL");
        assertEquals("Filter 'ALL' harus mengembalikan seluruh jurnal", 2, allList.size());

        // 5. Serialisasi dan Deserialisasi JSON (Save & Load)
        Path tempFile = Files.createTempFile("catalog_test_", ".json");
        try {
            JournalCatalog.save(tempFile, catalog.allJournals());
            assertTrue("File JSON katalog harus berhasil dibuat", Files.exists(tempFile));

            JournalCatalog loaded = JournalCatalog.load(tempFile);
            assertEquals("Katalog yang dimuat harus memiliki ukuran 2", 2, loaded.size());

            List<Journal> loadedEdu = loaded.filterBySubject("97");
            assertEquals("Katalog yang dimuat harus memiliki jurnal Education", 1, loadedEdu.size());
            assertEquals("Publisher jurnal sesuai", "UPI Bandung", loadedEdu.get(0).publisher());
            assertEquals("SINTA rank sesuai", "S1", loadedEdu.get(0).sintaRank());
            assertEquals("OJS URL sesuai", "https://upi.edu/pedagogik", loadedEdu.get(0).ojsUrl());
        } finally {
            Files.deleteIfExists(tempFile);
        }

        // 6. Pencarian Jurnal Berdasarkan Nama (Fuzzy/Normalized)
        Journal foundCs = catalog.findByName("Jurnal Ilmu Komputer");
        assertTrue("Harus menemukan jurnal berdasarkan nama", foundCs != null);
        assertEquals("ID jurnal cocok", "6001", foundCs.garudaId());

        Journal foundCsCase = catalog.findByName("jurnal ilmu komputer (JIK)");
        assertTrue("Harus menemukan jurnal mengabaikan kurung singkatan", foundCsCase != null);

        // 7. Resolusi SINTA Rank & Origin
        assertEquals("Resolusi SINTA rank dari URL", "S1", catalog.resolveSintaRank("https://upi.edu/pedagogik/article/view/123", "Unknown"));
        assertEquals("Resolusi SINTA rank dari nama jurnal", "S1", catalog.resolveSintaRank("", "Jurnal Pedagogik Modern"));

        // 8. Evaluasi Logika Filter SINTA (matchesSinta)
        assertTrue("ALL harus selalu lolos", JournalCatalog.matchesSinta("S3", "ALL"));
        assertTrue("S1 lolos minimal S1", JournalCatalog.matchesSinta("S1", "S1"));
        assertTrue("S1 lolos minimal S2", JournalCatalog.matchesSinta("S1", "S2"));
        assertTrue("S2 lolos minimal S2", JournalCatalog.matchesSinta("S2", "S2"));
        assertFalse("S3 tidak lolos minimal S2", JournalCatalog.matchesSinta("S3", "S2"));
        assertFalse("S6 tidak lolos minimal S3", JournalCatalog.matchesSinta("S6", "S3"));
        assertFalse("Unranked '-' tidak lolos filter SINTA spesifik", JournalCatalog.matchesSinta("-", "S2"));
        assertEquals("Parse angka SINTA dari 'S2'", 2, JournalCatalog.parseSintaNumber("S2"));
        assertEquals("Parse angka SINTA dari 'Sinta 5'", 5, JournalCatalog.parseSintaNumber("Sinta 5"));
    }
}
