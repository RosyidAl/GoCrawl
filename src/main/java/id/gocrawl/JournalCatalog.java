package id.gocrawl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manajer penyimpanan dan pemfilteran Master Journal Registry (data/journals_master.json).
 */
public final class JournalCatalog {

    private final Map<String, Journal> journalsById = new LinkedHashMap<>();
    private final Map<String, List<Journal>> journalsBySubjectId = new HashMap<>();

    public JournalCatalog() {}

    public synchronized void register(Journal journal) {
        if (journal == null || journal.garudaId().isBlank()) return;
        journalsById.put(journal.garudaId(), journal);
        if (!journal.subjectId().isBlank()) {
            journalsBySubjectId.computeIfAbsent(journal.subjectId(), k -> new ArrayList<>()).add(journal);
        }
    }

    public synchronized List<Journal> allJournals() {
        return new ArrayList<>(journalsById.values());
    }

    public synchronized Journal findByUrlOrHost(String url) {
        if (url == null || url.isBlank()) return null;
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String host = "";
        try {
            URI u = URI.create(url);
            if (u.getHost() != null) host = u.getHost().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {}

        for (Journal j : journalsById.values()) {
            if (j.ojsUrl() != null && !j.ojsUrl().isBlank()) {
                String ojsLower = j.ojsUrl().toLowerCase(Locale.ROOT);
                if (lowerUrl.startsWith(ojsLower) || ojsLower.startsWith(lowerUrl)) {
                    return j;
                }
                try {
                    URI ju = URI.create(j.ojsUrl());
                    if (ju.getHost() != null && !host.isEmpty() && host.equalsIgnoreCase(ju.getHost())) {
                        return j;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public synchronized Journal findByName(String name) {
        if (name == null || name.isBlank() || name.equals("-")) return null;
        String clean = normalizeForMatch(name);
        if (clean.isBlank()) return null;
        for (Journal j : journalsById.values()) {
            if (j.name() == null || j.name().isBlank()) continue;
            String jClean = normalizeForMatch(j.name());
            if (jClean.equals(clean) || (jClean.length() >= 5 && clean.length() >= 5 && (jClean.contains(clean) || clean.contains(jClean)))) {
                return j;
            }
        }
        return null;
    }

    private static String normalizeForMatch(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[^a-z0-9]", "")
                .strip();
    }

    public synchronized String resolveSintaRank(String url, String journalName) {
        Journal j = findByUrlOrHost(url);
        if (j == null && journalName != null) {
            j = findByName(journalName);
        }
        if (j != null && j.sintaRank() != null && !j.sintaRank().isBlank() && !j.sintaRank().equals("-")) {
            return j.sintaRank();
        }
        return "-";
    }

    public synchronized String resolveSubject(String url, String journalName) {
        Journal j = findByUrlOrHost(url);
        if (j == null && journalName != null) {
            j = findByName(journalName);
        }
        if (j != null && j.subjectName() != null && !j.subjectName().isBlank() && !j.subjectName().equals("-")) {
            return j.subjectName();
        }
        return "-";
    }

    public synchronized String resolveCoreSubject(String url, String journalName) {
        Journal j = findByUrlOrHost(url);
        if (j == null && journalName != null) {
            j = findByName(journalName);
        }
        if (j != null && j.coreSubject() != null && !j.coreSubject().isBlank() && !j.coreSubject().equals("-")) {
            return j.coreSubject();
        }
        return "-";
    }

    public static boolean matchesSinta(String rank, String minFilter) {
        if (minFilter == null || minFilter.isBlank() || minFilter.equalsIgnoreCase("ALL")) {
            return true;
        }
        if (rank == null || rank.isBlank() || rank.equals("-")) {
            return false;
        }
        int rankNum = parseSintaNumber(rank);
        int filterNum = parseSintaNumber(minFilter);
        if (rankNum <= 0 || filterNum <= 0) return false;
        return rankNum <= filterNum;
    }

    public static int parseSintaNumber(String s) {
        if (s == null) return -1;
        Matcher m = Pattern.compile("(?i)s(?:inta)?\\s*([1-6])").matcher(s);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public synchronized String resolveOrigin(String url, String journalName) {
        Journal j = findByUrlOrHost(url);
        if (j == null && journalName != null) {
            j = findByName(journalName);
        }
        if (j != null && j.publisher() != null && !j.publisher().isBlank() && !j.publisher().equals("-")) {
            return j.publisher();
        }
        if (url != null) {
            try {
                URI u = URI.create(url);
                if (u.getHost() != null && !u.getHost().isBlank()) {
                    return u.getHost();
                }
            } catch (Exception ignored) {}
        }
        return "-";
    }

    public synchronized int size() {
        return journalsById.size();
    }

    public synchronized List<Journal> filterBySubject(String subjectIdOrName) {
        if (subjectIdOrName == null || subjectIdOrName.isBlank() || subjectIdOrName.equalsIgnoreCase("ALL")) {
            return allJournals();
        }
        String resolvedId = GarudaSubject.resolveSubjectId(subjectIdOrName);
        List<Journal> result = new ArrayList<>();
        for (Journal j : journalsById.values()) {
            if (resolvedId != null && j.subjectId().equals(resolvedId)) {
                result.add(j);
            } else if (j.subjectName().equalsIgnoreCase(subjectIdOrName)
                    || j.subjectName().toLowerCase(Locale.ROOT).contains(subjectIdOrName.toLowerCase(Locale.ROOT))) {
                result.add(j);
            }
        }
        return result;
    }

    public static JournalCatalog load(Path file) throws IOException {
        JournalCatalog catalog = new JournalCatalog();
        if (!Files.exists(file)) {
            return catalog;
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<Journal> list = parseJson(content);
        for (Journal j : list) {
            catalog.register(j);
        }
        return catalog;
    }

    public static void save(Path file, Collection<Journal> journals) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("[\n");
            int i = 0;
            int total = journals.size();
            for (Journal j : journals) {
                writer.write("  {\n");
                writer.write("    \"id\": \"" + jsonEscape(j.id()) + "\",\n");
                writer.write("    \"name\": \"" + jsonEscape(j.name()) + "\",\n");
                writer.write("    \"publisher\": \"" + jsonEscape(j.publisher()) + "\",\n");
                writer.write("    \"garudaId\": \"" + jsonEscape(j.garudaId()) + "\",\n");
                writer.write("    \"coreSubject\": \"" + jsonEscape(j.coreSubject() != null ? j.coreSubject() : "-") + "\",\n");
                writer.write("    \"subjectId\": \"" + jsonEscape(j.subjectId()) + "\",\n");
                writer.write("    \"subjectName\": \"" + jsonEscape(j.subjectName()) + "\",\n");
                writer.write("    \"sintaRank\": \"" + jsonEscape(j.sintaRank()) + "\",\n");
                writer.write("    \"ojsUrl\": \"" + jsonEscape(j.ojsUrl()) + "\",\n");
                writer.write("    \"oaiUrl\": \"" + jsonEscape(j.oaiUrl()) + "\",\n");
                writer.write("    \"issn\": \"" + jsonEscape(j.issn()) + "\",\n");
                writer.write("    \"eissn\": \"" + jsonEscape(j.eissn()) + "\",\n");
                writer.write("    \"isActive\": " + j.isActive() + "\n");
                writer.write("  }" + (++i < total ? ",\n" : "\n"));
            }
            writer.write("]\n");
        }
    }

    /**
     * Parser JSON ringan khusus skema Journal tanpa dependensi library eksternal.
     */
    public static List<Journal> parseJson(String json) {
        List<Journal> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        Pattern objectPattern = Pattern.compile("\\{([^{}]+)\\}");
        Matcher objectMatcher = objectPattern.matcher(json);

        while (objectMatcher.find()) {
            String objBody = objectMatcher.group(1);
            Map<String, String> fields = extractFields(objBody);
            String garudaId = fields.getOrDefault("garudaId", "");
            if (garudaId.isBlank()) continue;

            Journal journal = new Journal(
                    fields.getOrDefault("id", "garuda-" + garudaId),
                    fields.getOrDefault("name", ""),
                    fields.getOrDefault("publisher", ""),
                    garudaId,
                    fields.getOrDefault("coreSubject", "-"),
                    fields.getOrDefault("subjectId", ""),
                    fields.getOrDefault("subjectName", ""),
                    fields.getOrDefault("sintaRank", "-"),
                    fields.getOrDefault("ojsUrl", ""),
                    fields.getOrDefault("oaiUrl", ""),
                    fields.getOrDefault("issn", "-"),
                    fields.getOrDefault("eissn", "-"),
                    Boolean.parseBoolean(fields.getOrDefault("isActive", "true"))
            );
            result.add(journal);
        }
        return result;
    }

    private static Map<String, String> extractFields(String body) {
        Map<String, String> fields = new HashMap<>();
        Pattern fieldPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,\\s]+))");
        Matcher m = fieldPattern.matcher(body);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2) != null ? m.group(2) : m.group(3);
            fields.put(key, unescapeJson(val == null ? "" : val));
        }
        return fields;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
