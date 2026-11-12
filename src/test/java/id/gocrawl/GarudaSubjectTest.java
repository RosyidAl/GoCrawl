package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class GarudaSubjectTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] GarudaSubjectTest");

        // 1. Verifikasi total subjek resmi 40
        assertEquals("Total subjek resmi Portal Garuda harus 40", 40, GarudaSubject.OFFICIAL_SUBJECTS.size());

        // 2. Pencarian ID langsung
        assertEquals("Lookup ID 60", "60", GarudaSubject.resolveSubjectId("60"));
        assertEquals("Lookup ID 97", "97", GarudaSubject.resolveSubjectId("97"));
        assertEquals("Lookup ID 1", "1", GarudaSubject.resolveSubjectId("1"));

        // 3. Pencarian Nama Eksak dan Fleksibel
        assertEquals("Lookup nama Computer Science & IT", "60", GarudaSubject.resolveSubjectId("Computer Science & IT"));
        assertEquals("Lookup nama Education (case-insensitive)", "97", GarudaSubject.resolveSubjectId("education"));
        assertEquals("Lookup nama Religion (case-insensitive)", "1", GarudaSubject.resolveSubjectId("religion"));
        assertEquals("Lookup parsial computer", "60", GarudaSubject.resolveSubjectId("computer"));

        // 4. Kueri tidak valid / null / blank
        assertEquals("Lookup subjek fiktif harus null", null, GarudaSubject.resolveSubjectId("FiktifXYZ123"));
        assertEquals("Lookup null harus null", null, GarudaSubject.resolveSubjectId(null));
        assertEquals("Lookup blank harus null", null, GarudaSubject.resolveSubjectId("   "));

        // 5. Nama Subjek terdaftar
        assertEquals("Nama resmi ID 60", "Computer Science & IT", GarudaSubject.getSubjectName("60"));
        assertEquals("Nama resmi ID 97", "Education", GarudaSubject.getSubjectName("97"));
        assertEquals("Nama resmi ID tidak dikenal", "Unknown Subject", GarudaSubject.getSubjectName("9999"));

        // 6. Pencarian dengan Padanan Istilah Bahasa Indonesia (Aliases)
        assertEquals("Lookup padanan 'humaniora' harus ID 24", "24", GarudaSubject.resolveSubjectId("humaniora"));
        assertEquals("Lookup padanan 'agama' harus ID 1", "1", GarudaSubject.resolveSubjectId("agama"));
        assertEquals("Lookup padanan 'pendidikan' harus ID 97", "97", GarudaSubject.resolveSubjectId("pendidikan"));
        assertEquals("Lookup padanan 'komputer' harus ID 60", "60", GarudaSubject.resolveSubjectId("komputer"));
        assertEquals("Lookup padanan 'teknik' harus ID 115", "115", GarudaSubject.resolveSubjectId("teknik"));

        // 7. Label Bilingual GUI
        assertTrue("Display label ID 24 harus memuat Humaniora", GarudaSubject.getDisplayLabel("24").contains("Humaniora"));
        assertTrue("Display label ID 1 harus memuat Agama", GarudaSubject.getDisplayLabel("1").contains("Agama"));
        assertTrue("Display label ID 97 harus memuat Pendidikan", GarudaSubject.getDisplayLabel("97").contains("Pendidikan"));

        // 8. SubjectScopeItem GUI model
        GarudaSubject.SubjectScopeItem scopeItem = new GarudaSubject.SubjectScopeItem("24", "Humanities (Humaniora)");
        assertEquals("Scope item ID", "24", scopeItem.id());
        assertEquals("Scope item toString harus displayName", "Humanities (Humaniora)", scopeItem.toString());
    }
}
