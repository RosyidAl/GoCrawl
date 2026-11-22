package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.ArticleStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Menangani state workspace dan persistence proyek/koleksi artikel. */
final class WorkspaceController {

    private final Path projectsRoot;
    private final Path defaultWorkspace;
    private final Path searchOutput;
    private final Map<Path, Integer> countCache = new ConcurrentHashMap<>();
    private Path activeProject;

    WorkspaceController() {
        this(Path.of("projects"), Path.of("."), Path.of("output", "garuda_search"));
    }

    WorkspaceController(Path projectsRoot, Path defaultWorkspace, Path searchOutput) {
        this.projectsRoot = projectsRoot.toAbsolutePath().normalize();
        this.defaultWorkspace = defaultWorkspace.toAbsolutePath().normalize();
        this.searchOutput = searchOutput;
        this.activeProject = this.defaultWorkspace;
    }

    Path activeProject() {
        return activeProject;
    }

    void setActiveProject(Path path) {
        activeProject = path.toAbsolutePath().normalize();
    }

    Path defaultWorkspace() {
        return defaultWorkspace;
    }

    Path collectionStore(Path collection) {
        Path normalized = collection.toAbsolutePath().normalize();
        return normalized.equals(defaultWorkspace) ? searchOutput : normalized;
    }

    Path favoritesPath() {
        return projectsRoot.resolve("Favorit").toAbsolutePath().normalize();
    }

    void ensureFavorites() throws IOException {
        Files.createDirectories(favoritesPath());
    }

    List<Path> sidebarCollections() {
        List<Path> collections = new ArrayList<>();
        try (var stream = Files.list(projectsRoot)) {
            Path favorites = favoritesPath();
            stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .sorted()
                    .filter(path -> !path.equals(favorites))
                    .forEach(collections::add);
        } catch (Exception ignored) {
            // Folder projects/ belum ada atau tidak dapat dibaca.
        }
        return collections;
    }

    List<Path> userCollections() {
        List<Path> collections = new ArrayList<>();
        try (var stream = Files.list(projectsRoot)) {
            stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .sorted()
                    .forEach(collections::add);
        } catch (Exception ignored) {
            // Tidak ada koleksi yang tersedia.
        }
        return collections;
    }

    int articleCount(Path collection) {
        Path normalized = collection.toAbsolutePath().normalize();
        return countCache.computeIfAbsent(normalized, path -> {
            try {
                return ArticleStorage.scanWorkspace(collectionStore(path)).size();
            } catch (Exception ignored) {
                return 0;
            }
        });
    }

    void invalidate(Path collection) {
        if (collection == null) {
            countCache.clear();
        } else {
            countCache.remove(collection.toAbsolutePath().normalize());
        }
    }

    String displayName(Path collection) {
        Path normalized = collection.toAbsolutePath().normalize();
        if (normalized.equals(defaultWorkspace)) return "Default workspace";
        Path fileName = normalized.getFileName();
        return fileName == null ? normalized.toString() : fileName.toString();
    }

    boolean canDelete(Path collection) {
        Path normalized = collection.toAbsolutePath().normalize();
        return normalized.startsWith(projectsRoot) && !normalized.equals(projectsRoot);
    }

    Path createCollection(String name) throws IOException {
        if (name == null) throw new IllegalArgumentException("Nama koleksi tidak boleh kosong.");
        String cleanName = name.strip();
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")
                || cleanName.equals(".") || cleanName.equals("..")) {
            throw new IllegalArgumentException("Masukkan nama koleksi yang valid.");
        }
        Path collection = projectsRoot.resolve(cleanName).normalize();
        if (!projectsRoot.equals(collection.getParent())) {
            throw new IllegalArgumentException("Nama koleksi tidak valid.");
        }
        Files.createDirectories(collection);
        invalidate(null);
        return collection;
    }

    void deleteCollection(Path collection) throws IOException {
        Path normalized = collection.toAbsolutePath().normalize();
        if (!canDelete(normalized)) return;
        try (var walk = Files.walk(normalized)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Satu berkas yang gagal dihapus tidak boleh merusak navigasi GUI.
                }
            });
        }
        invalidate(null);
        if (activeProject.equals(normalized)) {
            activeProject = defaultWorkspace;
        }
    }

    List<Article> loadCollection(Path collection) {
        return ArticleStorage.scanWorkspace(collectionStore(collection));
    }

    void saveJson(Path file, List<Article> articles) throws IOException {
        ArticleStorage.saveJson(file, articles);
        invalidate(file.getParent());
    }

    void saveSearchOutput(List<Article> articles) throws IOException {
        Files.createDirectories(searchOutput);
        ArticleStorage.saveJson(searchOutput.resolve("articles.json"), articles);
        invalidate(defaultWorkspace);
    }

    boolean addToCollection(Path collection, Article article) throws IOException {
        Files.createDirectories(collection);
        Path json = collection.resolve("articles.json");
        List<Article> existing = readArticles(json);
        if (articleExists(existing, article)) return false;
        existing.add(article);
        ArticleStorage.saveJson(json, existing);
        invalidate(collection);
        return true;
    }

    List<Article> removeFromCollection(Path collection, Article article) throws IOException {
        Path json = collectionStore(collection).resolve("articles.json");
        List<Article> existing = readArticles(json);
        existing.removeIf(saved -> sameArticle(saved, article));
        ArticleStorage.saveJson(json, existing);
        invalidate(collection);
        return existing;
    }

    private static List<Article> readArticles(Path json) throws IOException {
        if (!Files.exists(json)) return new ArrayList<>();
        return new ArrayList<>(ArticleStorage.parseJson(Files.readString(json, StandardCharsets.UTF_8)));
    }

    private static boolean articleExists(List<Article> articles, Article article) {
        return articles.stream().anyMatch(saved -> sameArticle(saved, article));
    }

    static boolean sameArticle(Article first, Article second) {
        return (!first.url().isBlank() && first.url().equalsIgnoreCase(second.url()))
                || (!first.doi().isBlank() && first.doi().equalsIgnoreCase(second.doi()))
                || first.title().equalsIgnoreCase(second.title());
    }
}
