package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.JournalCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static id.gocrawl.TestSupport.*;

public class ArticleExportServiceTest {

    public static void run() throws IOException {
        System.out.println("\n[TEST SUITE] ArticleExportServiceTest");

        Article article = new Article(
                "Judul, dengan koma", "Penulis Satu", "2024", "Jurnal Uji",
                "https://example.org/article/view/1", "Abstrak dengan\nbaris baru", "-",
                "2024-01-01", "uji");
        JournalCatalog catalog = new JournalCatalog();
        Path csv = Files.createTempFile("gocrawl_export_", ".csv");
        Path json = Files.createTempFile("gocrawl_export_", ".json");
        try {
            ArticleExportService.export(csv, ArticleExportService.Format.CSV, List.of(article), catalog);
            ArticleExportService.export(json, ArticleExportService.Format.JSON, List.of(article), catalog);

            String csvText = Files.readString(csv);
            String jsonText = Files.readString(json);
            assertTrue("CSV harus memiliki header artikel", csvText.startsWith("\"Title\",\"Journal\""));
            assertTrue("CSV harus meng-escape koma dalam judul", csvText.contains("\"Judul, dengan koma\""));
            assertTrue("JSON harus memiliki judul artikel", jsonText.contains("\"title\": \"Judul, dengan koma\""));
            assertTrue("JSON harus meng-escape baris baru", jsonText.contains("Abstrak dengan\\nbaris baru"));
        } finally {
            Files.deleteIfExists(csv);
            Files.deleteIfExists(json);
        }
    }
}
