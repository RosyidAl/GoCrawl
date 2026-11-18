package id.gocrawl.gui;

import id.gocrawl.JournalCatalog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static id.gocrawl.TestSupport.*;

public class SearchControllerTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] SearchControllerTest");

        SearchController.Request request = new SearchController.Request("  machine learning  ", null, null, null, 0);
        assertEquals("Keyword request harus dinormalisasi", "machine learning", request.keyword());
        assertEquals("SINTA default harus ALL", "ALL", request.sintaRank());
        assertEquals("Ukuran halaman minimum harus satu", 1, request.pageSize());

        List<String> events = new ArrayList<>();
        SearchController.Listener listener = new SearchController.Listener() {
            public void onStarted(String status) { events.add("started"); }
            public void onProgress(String status) { events.add("progress"); }
            public void onFinished() { events.add("finished"); }
            public void onPageLoaded(SearchController.Page page) { events.add("page"); }
            public void onCancelled() { events.add("cancelled"); }
            public void onFailure(String message) { events.add("failure"); }
            public void onSourceUnavailable() { events.add("unavailable"); }
            public void onSourceFallback(String message) { events.add("fallback"); }
        };

        SearchController controller = new SearchController(
                new JournalCatalog(), new ArrayList<>(), Path.of("target", "test-sources.txt"), listener);
        controller.start(request);
        assertTrue("Sumber kosong harus menghasilkan event unavailable", events.contains("unavailable"));
        assertFalse("Controller tanpa sumber tidak boleh menjalankan worker", controller.isRunning());
        assertEquals("Halaman awal kosong harus stabil", 0, controller.visiblePage().articles().size());
    }
}
