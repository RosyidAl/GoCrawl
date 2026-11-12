package id.gocrawl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pemetaan resmi 40 subjek keilmuan Portal Garuda (garuda.kemdiktisaintek.go.id/area)
 * dilengkapi padanan istilah bahasa Indonesia dan label bilingual untuk antarmuka.
 */
public record GarudaSubject(String id, String name) {

    public static final Map<String, String> OFFICIAL_SUBJECTS;
    public static final Map<String, String> BILINGUAL_LABELS;
    private static final Map<String, String> INDONESIAN_ALIASES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("60", "Computer Science & IT");
        map.put("97", "Education");
        map.put("1", "Religion");
        map.put("95", "Economics, Econometrics & Finance");
        map.put("190", "Social Sciences");
        map.put("24", "Humanities");
        map.put("137", "Law, Crime, Criminology & Criminal Justice");
        map.put("135", "Languange, Linguistic, Communication & Media");
        map.put("80", "Decision Sciences, Operations Research & Management");
        map.put("185", "Public Health");
        map.put("170", "Medicine & Pharmacology");
        map.put("177", "Nursing");
        map.put("125", "Health Professions");
        map.put("85", "Dentistry");
        map.put("20", "Agriculture, Biological Sciences & Forestry");
        map.put("115", "Engineering");
        map.put("50", "Civil Engineering, Building, Construction & Architecture");
        map.put("100", "Electrical & Electronics Engineering");
        map.put("160", "Mechanical Engineering");
        map.put("132", "Industrial & Manufacturing Engineering");
        map.put("40", "Chemical Engineering, Chemistry & Bioengineering");
        map.put("70", "Control & Systems Engineering");
        map.put("30", "Automotive Engineering");
        map.put("10", "Aerospace Engineering");
        map.put("120", "Environmental Science");
        map.put("110", "Energy");
        map.put("150", "Mathematics");
        map.put("45", "Chemistry");
        map.put("180", "Physics");
        map.put("26", "Astronomy");
        map.put("90", "Earth & Planetary Sciences");
        map.put("35", "Biochemistry, Genetics & Molecular Biology");
        map.put("130", "Immunology & microbiology");
        map.put("175", "Neuroscience");
        map.put("140", "Materials Science & Nanotechnology");
        map.put("200", "Transportation");
        map.put("220", "Veterinary");
        map.put("138", "Library & Information Science");
        map.put("23", "Arts");
        map.put("500", "Other");
        OFFICIAL_SUBJECTS = Collections.unmodifiableMap(map);

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("24", "Humanities (Humaniora)");
        labels.put("1", "Religion (Agama & Keislaman)");
        labels.put("97", "Education (Pendidikan)");
        labels.put("60", "Computer Science & IT (Ilmu Komputer)");
        labels.put("190", "Social Sciences (Ilmu Sosial)");
        labels.put("95", "Economics, Econometrics & Finance (Ekonomi & Keuangan)");
        labels.put("137", "Law & Criminology (Hukum & Kriminologi)");
        labels.put("135", "Language & Communication (Bahasa & Media)");
        labels.put("80", "Management & Operations (Manajemen)");
        labels.put("185", "Public Health (Kesehatan Masyarakat)");
        labels.put("170", "Medicine & Pharmacology (Kedokteran & Farmasi)");
        labels.put("177", "Nursing (Keperawatan)");
        labels.put("125", "Health Professions (Tenaga Kesehatan)");
        labels.put("85", "Dentistry (Kedokteran Gigi)");
        labels.put("20", "Agriculture & Forestry (Pertanian & Kehutanan)");
        labels.put("115", "Engineering (Teknik & Rekayasa)");
        labels.put("50", "Civil Engineering & Architecture (Sipil & Arsitektur)");
        labels.put("100", "Electrical & Electronics (Teknik Elektro)");
        labels.put("160", "Mechanical Engineering (Teknik Mesin)");
        labels.put("132", "Industrial Engineering (Teknik Industri)");
        labels.put("40", "Chemical Engineering (Teknik Kimia)");
        labels.put("70", "Control & Systems (Sistem & Kontrol)");
        labels.put("30", "Automotive Engineering (Teknik Otomotif)");
        labels.put("10", "Aerospace Engineering (Kedirgantaraan)");
        labels.put("120", "Environmental Science (Ilmu Lingkungan)");
        labels.put("110", "Energy (Energi)");
        labels.put("150", "Mathematics (Matematika)");
        labels.put("45", "Chemistry (Kimia)");
        labels.put("180", "Physics (Fisika)");
        labels.put("26", "Astronomy (Astronomi)");
        labels.put("90", "Earth & Planetary Sciences (Ilmu Kebumian)");
        labels.put("35", "Biochemistry & Genetics (Biokimia & Genetika)");
        labels.put("130", "Immunology & Microbiology (Imunologi & Mikrobiologi)");
        labels.put("175", "Neuroscience (Neurosains)");
        labels.put("140", "Materials Science (Sains Material)");
        labels.put("200", "Transportation (Transportasi)");
        labels.put("220", "Veterinary (Kedokteran Hewan)");
        labels.put("138", "Library Science (Ilmu Perpustakaan)");
        labels.put("23", "Arts (Seni & Desain)");
        labels.put("500", "Other (Lainnya)");
        BILINGUAL_LABELS = Collections.unmodifiableMap(labels);

        Map<String, String> aliases = new HashMap<>();
        aliases.put("humaniora", "24");
        aliases.put("humanities", "24");
        aliases.put("budaya", "24");
        aliases.put("sastra", "24");
        aliases.put("sejarah", "24");
        aliases.put("agama", "1");
        aliases.put("religion", "1");
        aliases.put("islam", "1");
        aliases.put("studi islam", "1");
        aliases.put("pendidikan", "97");
        aliases.put("education", "97");
        aliases.put("tarbiyah", "97");
        aliases.put("komputer", "60");
        aliases.put("it", "60");
        aliases.put("informatika", "60");
        aliases.put("ekonomi", "95");
        aliases.put("keuangan", "95");
        aliases.put("bisnis", "95");
        aliases.put("sosial", "190");
        aliases.put("ilmu sosial", "190");
        aliases.put("hukum", "137");
        aliases.put("bahasa", "135");
        aliases.put("linguistik", "135");
        aliases.put("komunikasi", "135");
        aliases.put("manajemen", "80");
        aliases.put("kesehatan", "185");
        aliases.put("kesehatan masyarakat", "185");
        aliases.put("kedokteran", "170");
        aliases.put("farmasi", "170");
        aliases.put("keperawatan", "177");
        aliases.put("gigi", "85");
        aliases.put("pertanian", "20");
        aliases.put("kehutanan", "20");
        aliases.put("biologi", "20");
        aliases.put("teknik", "115");
        aliases.put("rekayasa", "115");
        aliases.put("sipil", "50");
        aliases.put("arsitektur", "50");
        aliases.put("elektro", "100");
        aliases.put("mesin", "160");
        aliases.put("industri", "132");
        aliases.put("lingkungan", "120");
        aliases.put("energi", "110");
        aliases.put("matematika", "150");
        aliases.put("kimia", "45");
        aliases.put("fisika", "180");
        aliases.put("astronomi", "26");
        aliases.put("kebumian", "90");
        aliases.put("biokimia", "35");
        aliases.put("genetika", "35");
        aliases.put("imunologi", "130");
        aliases.put("mikrobiologi", "130");
        aliases.put("neurosains", "175");
        aliases.put("material", "140");
        aliases.put("transportasi", "200");
        aliases.put("hewan", "220");
        aliases.put("kedokteran hewan", "220");
        aliases.put("perpustakaan", "138");
        aliases.put("seni", "23");
        aliases.put("desain", "23");
        INDONESIAN_ALIASES = Collections.unmodifiableMap(aliases);
    }

    /**
     * Mencari ID subjek berdasarkan ID numerik, nama resmi, atau istilah padanan bahasa Indonesia.
     */
    public static String resolveSubjectId(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.strip();
        if (OFFICIAL_SUBJECTS.containsKey(q)) {
            return q;
        }
        String lower = q.toLowerCase(Locale.ROOT);
        if (INDONESIAN_ALIASES.containsKey(lower)) {
            return INDONESIAN_ALIASES.get(lower);
        }
        for (Map.Entry<String, String> e : OFFICIAL_SUBJECTS.entrySet()) {
            if (e.getValue().toLowerCase(Locale.ROOT).contains(lower) || lower.contains(e.getValue().toLowerCase(Locale.ROOT))) {
                return e.getKey();
            }
        }
        for (Map.Entry<String, String> e : INDONESIAN_ALIASES.entrySet()) {
            if (lower.contains(e.getKey()) || e.getKey().contains(lower)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String getSubjectName(String id) {
        return OFFICIAL_SUBJECTS.getOrDefault(id, "Unknown Subject");
    }

    public static String getDisplayLabel(String id) {
        if (id == null) return "Unknown Subject";
        if (BILINGUAL_LABELS.containsKey(id)) {
            return BILINGUAL_LABELS.get(id);
        }
        return getSubjectName(id);
    }

    /**
     * Objek pemilih lingkup bidang ilmu untuk JComboBox antarmuka GUI.
     */
    public record SubjectScopeItem(String id, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
