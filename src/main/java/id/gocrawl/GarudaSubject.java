package id.gocrawl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pemetaan resmi 40 subjek keilmuan Portal Garuda (garuda.kemdiktisaintek.go.id/area).
 */
public record GarudaSubject(String id, String name) {

    public static final Map<String, String> OFFICIAL_SUBJECTS;

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
    }

    /**
     * Mencari ID subjek berdasarkan ID langsung atau pencarian nama yang fleksibel.
     */
    public static String resolveSubjectId(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.strip();
        if (OFFICIAL_SUBJECTS.containsKey(q)) {
            return q;
        }
        String lower = q.toLowerCase();
        for (Map.Entry<String, String> e : OFFICIAL_SUBJECTS.entrySet()) {
            if (e.getValue().toLowerCase().contains(lower)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static String getSubjectName(String id) {
        return OFFICIAL_SUBJECTS.getOrDefault(id, "Unknown Subject");
    }
}
