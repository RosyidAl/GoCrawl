package id.gocrawl;

/**
 * Representasi entitas jurnal ilmiah di dalam Master Journal Registry.
 */
public record Journal(
        String id,
        String name,
        String publisher,
        String garudaId,
        String subjectId,
        String subjectName,
        String sintaRank,
        String ojsUrl,
        String oaiUrl,
        String issn,
        String eissn,
        boolean isActive
) {
    public static Journal create(
            String garudaId,
            String name,
            String publisher,
            String subjectId,
            String subjectName,
            String ojsUrl,
            String oaiUrl,
            String issn,
            String eissn
    ) {
        String cleanName = name == null ? "" : name.replace("\"", "'").replaceAll("^'+|'+$", "").strip();
        String id = "garuda-" + (garudaId == null ? "" : garudaId.strip());
        return new Journal(
                id,
                cleanName,
                publisher == null ? "" : publisher.strip(),
                garudaId == null ? "" : garudaId.strip(),
                subjectId == null ? "" : subjectId.strip(),
                subjectName == null ? "" : subjectName.strip(),
                "-", // Peringkat SINTA default, akan diisi pada fase enrichment
                ojsUrl == null ? "" : ojsUrl.strip(),
                oaiUrl == null ? "" : oaiUrl.strip(),
                issn == null ? "-" : issn.strip(),
                eissn == null ? "-" : eissn.strip(),
                true
        );
    }
}
