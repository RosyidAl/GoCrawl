package id.gocrawl.gui;

import id.gocrawl.Article;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static id.gocrawl.TestSupport.*;

public class WorkspaceControllerTest {

    public static void run() throws IOException {
        System.out.println("\n[TEST SUITE] WorkspaceControllerTest");

        Path root = Files.createTempDirectory("gocrawl_workspace_");
        try {
            WorkspaceController workspace = new WorkspaceController(
                    root.resolve("projects"), root.resolve("default"), root.resolve("output"));
            Article article = new Article(
                    "Artikel Workspace", "Penulis Satu", "2024", "Jurnal Uji",
                    "https://example.org/article/view/1", "Abstrak", "10.1234/workspace.1",
                    "2024-01-01", "uji");

            Path collection = workspace.createCollection("Demo");
            assertTrue("Koleksi baru harus dibuat", Files.isDirectory(collection));
            assertEquals("Nama koleksi harus stabil", "Demo", workspace.displayName(collection));

            assertTrue("Artikel pertama harus berhasil ditambahkan", workspace.addToCollection(collection, article));
            assertFalse("Artikel yang sama tidak boleh ditambahkan dua kali", workspace.addToCollection(collection, article));
            assertEquals("Koleksi harus memuat satu artikel", 1, workspace.loadCollection(collection).size());

            List<Article> afterRemove = workspace.removeFromCollection(collection, article);
            assertTrue("Artikel harus terhapus dari koleksi", afterRemove.isEmpty());
            assertTrue("Default workspace harus aman dari penghapusan", !workspace.canDelete(workspace.defaultWorkspace()));
        } finally {
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }
}
