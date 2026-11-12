package id.gocrawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modul pemanen direktori resmi Portal Garuda (garuda.kemdiktisaintek.go.id).
 * Mengekstrak profil jurnal, afiliasi penerbit, subjek resmi, dan tautan OJS/OAI langsung.
 */
public final class GarudaHarvester {

    private static final String BASE_URL = "https://garuda.kemdiktisaintek.go.id";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 8000;
    private static final int POLITE_DELAY_MS = 50;

    private static final Pattern ISSN_PATTERN = Pattern.compile("ISSN\\s*:\\s*([^\\s\\u00a0<]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EISSN_PATTERN = Pattern.compile("EISSN\\s*:\\s*([^\\s\\u00a0<]+)", Pattern.CASE_INSENSITIVE);

    public record HarvestOptions(
            String subjectFilter,
            boolean harvestAll,
            Integer limitPerSubject,
            Path outputFile
    ) {}

    public static void harvest(HarvestOptions options) throws Exception {
        Path targetFile = options.outputFile() != null ? options.outputFile() : Path.of("data", "journals_master.json");
        JournalCatalog catalog = JournalCatalog.load(targetFile);
        System.out.println("[HARVEST] Memuat katalog awal: " + catalog.size() + " jurnal sudah terdaftar.");

        List<String> targetSubjectIds = new ArrayList<>();
        if (options.harvestAll()) {
            targetSubjectIds.addAll(GarudaSubject.OFFICIAL_SUBJECTS.keySet());
            System.out.println("[HARVEST] Mode Menyeluruh (--all): Menargetkan " + targetSubjectIds.size() + " subjek resmi Garuda.");
        } else if (options.subjectFilter() != null && !options.subjectFilter().isBlank()) {
            String resolvedId = GarudaSubject.resolveSubjectId(options.subjectFilter());
            if (resolvedId == null) {
                throw new IllegalArgumentException("Subjek tidak dikenali: '" + options.subjectFilter() +
                        "'. Gunakan flag --list-fields untuk melihat 40 subjek resmi Garuda.");
            }
            targetSubjectIds.add(resolvedId);
            System.out.println("[HARVEST] Mode Subjek Spesifik: " + resolvedId + " (" + GarudaSubject.getSubjectName(resolvedId) + ")");
        } else {
            // Default jika tidak ada opsi: panen Computer Science & IT (ID 60) dengan batas 20 untuk pengujian
            targetSubjectIds.add("60");
            System.out.println("[HARVEST] Subjek tidak dispesifikasikan. Memanen default: 60 (" + GarudaSubject.getSubjectName("60") + ")");
        }

        int totalHarvested = 0;
        for (String subjectId : targetSubjectIds) {
            String subjectName = GarudaSubject.getSubjectName(subjectId);
            System.out.println("\n------------------------------------------------------------");
            System.out.println("[SUBJEK] " + subjectId + ": " + subjectName);
            System.out.println("------------------------------------------------------------");

            int harvestedInSubject = harvestSubject(subjectId, subjectName, options.limitPerSubject(), catalog);
            totalHarvested += harvestedInSubject;
            System.out.println("[SUBJEK SELESAI] " + subjectName + ": " + harvestedInSubject + " jurnal baru/diperbarui.");

            // Simpan progres secara berkala per subjek
            JournalCatalog.save(targetFile, catalog.allJournals());
            System.out.println("[TERSIMPAN] Total katalog saat ini: " + catalog.size() + " jurnal di " + targetFile);
        }

        System.out.println("\n============================================================");
        System.out.println("[SELESAI] Panen katalog Garuda berhasil.");
        System.out.println("Total jurnal baru/diperbarui: " + totalHarvested);
        System.out.println("Total keseluruhan di katalog: " + catalog.size() + " jurnal.");
        System.out.println("Lokasi berkas data: " + targetFile.toAbsolutePath());
        System.out.println("============================================================");
    }

    private static int harvestSubject(String subjectId, String subjectName, Integer limit, JournalCatalog catalog) {
        int page = 1;
        int count = 0;

        while (true) {
            if (limit != null && count >= limit) {
                break;
            }

            String url = BASE_URL + "/area/index/" + subjectId + "?page=" + page;
            System.out.println("[HALAMAN] Mengakses " + url + " ...");

            Document doc;
            try {
                doc = fetchWithRetry(url);
            } catch (Exception e) {
                System.err.println("[GAGAL HALAMAN] " + url + ": " + e.getMessage());
                break;
            }

            Elements rows = doc.select("table tr, .ui.table tr");
            List<Element> journalRows = new ArrayList<>();
            for (Element row : rows) {
                if (row.selectFirst("a.title-journal") != null) {
                    journalRows.add(row);
                }
            }

            if (journalRows.isEmpty()) {
                System.out.println("[INFO] Tidak ada jurnal ditemukan di halaman " + page + ". Selesai menyisir subjek ini.");
                break;
            }

            for (Element row : journalRows) {
                if (limit != null && count >= limit) {
                    break;
                }

                Element titleEl = row.selectFirst("a.title-journal");
                if (titleEl == null) continue;

                String journalName = titleEl.text().strip();
                String viewHref = titleEl.attr("href").strip();
                String garudaId = extractGarudaId(viewHref);
                if (garudaId.isBlank()) continue;

                Element pubEl = row.selectFirst("a.subtitle-journal");
                String publisher = pubEl != null ? pubEl.text().strip() : "";

                String rowText = row.text();
                String issn = extractRegex(ISSN_PATTERN, rowText, "-");
                String eissn = extractRegex(EISSN_PATTERN, rowText, "-");

                // Buka halaman detail jurnal untuk mengambil Website URL (OJS), OAI Link, Core Subject, dan Multi-Subject
                String detailUrl = BASE_URL + (viewHref.startsWith("/") ? viewHref : "/" + viewHref);
                String ojsUrl = "";
                String oaiUrl = "";
                String coreSubject = "-";
                String extractedSubjectName = subjectName;
                String extractedSubjectId = subjectId;

                try {
                    sleep(POLITE_DELAY_MS);
                    Document detailDoc = fetchWithRetry(detailUrl);
                    Element homeLink = detailDoc.selectFirst("a:contains(Home Page)");
                    if (homeLink != null) {
                        ojsUrl = homeLink.attr("href").strip();
                    }
                    Element oaiLink = detailDoc.selectFirst("a:contains(OAI Link)");
                    if (oaiLink != null) {
                        oaiUrl = oaiLink.attr("href").strip();
                    }

                    // Ekstraksi Core Subject resmi Portal Garuda
                    Element coreSubEl = detailDoc.selectFirst(".j-meta-pub:contains(Core Subject)");
                    if (coreSubEl != null) {
                        String rawCore = coreSubEl.text().replaceFirst("(?i)Core Subject\\s*:\\s*", "").replaceAll("\\s+", " ").strip();
                        rawCore = rawCore.replaceAll(",\\s*$", "").strip();
                        if (!rawCore.isBlank()) {
                            coreSubject = rawCore;
                        }
                    }

                    // Ekstraksi Multi-Subject / Area Tags Portal Garuda
                    Elements subEls = detailDoc.select(".j-meta-subject a");
                    if (!subEls.isEmpty()) {
                        List<String> sNames = new ArrayList<>();
                        List<String> sIds = new ArrayList<>();
                        for (Element se : subEls) {
                            String sn = se.text().strip();
                            if (!sn.isBlank() && !sNames.contains(sn)) {
                                sNames.add(sn);
                                String sHref = se.attr("href");
                                String sid = extractGarudaId(sHref);
                                if (!sid.isBlank() && !sIds.contains(sid)) {
                                    sIds.add(sid);
                                }
                            }
                        }
                        if (!sNames.isEmpty()) {
                            extractedSubjectName = String.join(", ", sNames);
                        }
                        if (!sIds.isEmpty()) {
                            extractedSubjectId = String.join(", ", sIds);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("  [PERINGATAN] Gagal membuka detail jurnal " + garudaId + ": " + e.getMessage());
                }

                Journal journal = Journal.create(
                        garudaId,
                        journalName,
                        publisher,
                        coreSubject,
                        extractedSubjectId,
                        extractedSubjectName,
                        ojsUrl,
                        oaiUrl,
                        issn,
                        eissn
                );

                catalog.register(journal);
                count++;
                System.out.printf("  [%d] [%s] %s | %s -> %s\n", count, garudaId, journal.name(), journal.publisher(),
                        ojsUrl.isBlank() ? "(Tidak ada OJS URL)" : ojsUrl);
            }

            page++;
            sleep(POLITE_DELAY_MS);
        }

        return count;
    }

    private static String extractGarudaId(String href) {
        if (href == null) return "";
        Pattern p = Pattern.compile("/journal/view/(\\d+)");
        Matcher m = p.matcher(href);
        return m.find() ? m.group(1) : "";
    }

    private static String extractRegex(Pattern pattern, String text, String fallback) {
        if (text == null) return fallback;
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String val = m.group(1).strip();
            return val.isBlank() ? fallback : val;
        }
        return fallback;
    }

    private static Document fetchWithRetry(String url) throws IOException {
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                        .timeout(TIMEOUT_MS)
                        .get();
            } catch (IOException e) {
                if (i < maxRetries) {
                    sleep(200 * i);
                    continue;
                }
                throw e;
            }
        }
        throw new IOException("Gagal menghubungkan ke " + url);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
