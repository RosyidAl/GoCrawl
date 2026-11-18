package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import id.gocrawl.GarudaSearchEngine;
import id.gocrawl.JournalCatalog;
import id.gocrawl.OjsCrawler;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Mengelola state, pagination, dan worker asynchronous pencarian Garuda/OJS. */
final class SearchController {

    record Request(String keyword, String sintaRank, Integer fromYear, Integer toYear, int pageSize) {
        Request {
            keyword = keyword == null ? "" : keyword.strip();
            sintaRank = sintaRank == null || sintaRank.isBlank() ? "ALL" : sintaRank;
            pageSize = Math.max(1, pageSize);
        }
    }

    record Page(List<Article> articles, int page, int totalPages, int totalRecords) {
        Page {
            articles = articles == null ? List.of() : List.copyOf(articles);
        }
    }

    interface Listener {
        void onStarted(String status);

        void onProgress(String status);

        void onFinished();

        void onPageLoaded(Page page);

        void onCancelled();

        void onFailure(String message);

        void onSourceUnavailable();

        void onSourceFallback(String message);
    }

    private final JournalCatalog catalog;
    private final List<CrawlSource> crawlSources;
    private final Path sourceStoreFile;
    private final Listener listener;

    private final Map<Integer, List<Article>> pageCache = new ConcurrentHashMap<>();
    private final Map<Integer, GarudaSearchEngine.PageResult> garudaRawPageCache = new ConcurrentHashMap<>();
    private final List<Article> sintaFilteredPool = Collections.synchronizedList(new ArrayList<>());
    private final List<Article> ojsArticlePool = Collections.synchronizedList(new ArrayList<>());
    private final List<Article> masterArticleList = new ArrayList<>();
    private final List<String> ojsIssueQueue = new ArrayList<>();

    private SwingWorker<List<Article>, String> currentWorker;
    private Request activeRequest;
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalRecords = 0;
    private int totalGarudaPages = 1;
    private int totalGarudaRecords = 0;
    private int sintaLastGarudaPageScanned = 0;
    private int ojsIssuesScanned = 0;
    private boolean sintaPoolExhausted = false;
    private boolean ojsExhausted = false;
    private long generation = 0;

    SearchController(JournalCatalog catalog, List<CrawlSource> crawlSources,
                     Path sourceStoreFile, Listener listener) {
        this.catalog = catalog;
        this.crawlSources = crawlSources;
        this.sourceStoreFile = sourceStoreFile;
        this.listener = listener;
    }

    void start(Request request) {
        if (request.keyword().isBlank()) {
            listener.onFailure("Ketikkan kata kunci pencarian terlebih dahulu.");
            return;
        }
        if (activeSource() == null) {
            listener.onSourceUnavailable();
            return;
        }

        cancel();
        activeRequest = request;
        generation++;
        resetSearchState();
        loadPage(1);
    }

    void loadPage(int page) {
        if (activeRequest == null || activeRequest.keyword().isBlank()) {
            listener.onFailure("Ketikkan kata kunci pencarian terlebih dahulu.");
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        Page cached = cachedPage(page);
        if (cached != null) {
            currentPage = page;
            listener.onPageLoaded(cached);
            return;
        }

        CrawlSource source = activeSource();
        if (source == null) {
            listener.onSourceUnavailable();
            return;
        }
        if (CrawlSource.TYPE_OJS.equalsIgnoreCase(source.type())) {
            runOjsLoad(page, source);
            return;
        }

        long requestGeneration = generation;
        GarudaSearchEngine engine = new GarudaSearchEngine(source.baseUrl());
        listener.onStarted("Memuat Halaman " + page
                + (totalPages > 1 ? " dari " + totalPages : "")
                + " (" + activeRequest.pageSize() + " artikel/hal)...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected List<Article> doInBackground() throws Exception {
                boolean allSinta = "ALL".equalsIgnoreCase(activeRequest.sintaRank());
                if (allSinta) {
                    return loadGarudaPage(engine, page, message -> publish(message));
                }
                return loadSintaPage(engine, page, message -> publish(message));
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty() && requestGeneration == generation) {
                    listener.onProgress(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                if (requestGeneration == generation) {
                    finishPageLoad(page, this, source);
                }
            }
        };
        currentWorker.execute();
    }

    void setPageSize(int pageSize) {
        if (activeRequest == null || activeRequest.pageSize() == pageSize) return;
        activeRequest = new Request(activeRequest.keyword(), activeRequest.sintaRank(),
                activeRequest.fromYear(), activeRequest.toYear(), pageSize);
        pageCache.clear();
        if (!activeRequest.keyword().isBlank()) loadPage(1);
    }

    void cancel() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    }

    boolean isRunning() {
        return currentWorker != null && !currentWorker.isDone();
    }

    Page visiblePage() {
        Page cached = cachedPage(currentPage);
        if (cached != null) return cached;
        if (!masterArticleList.isEmpty()) {
            return new Page(masterArticleList, 1, 1, masterArticleList.size());
        }
        return new Page(List.of(), 1, 1, 0);
    }

    List<Article> accumulatedArticles() {
        return new ArrayList<>(masterArticleList);
    }

    int currentPage() {
        return currentPage;
    }

    int totalPages() {
        return totalPages;
    }

    int totalRecords() {
        return totalRecords;
    }

    private void resetSearchState() {
        currentPage = 1;
        totalPages = 1;
        totalRecords = 0;
        totalGarudaPages = 1;
        totalGarudaRecords = 0;
        sintaLastGarudaPageScanned = 0;
        ojsIssuesScanned = 0;
        sintaPoolExhausted = false;
        ojsExhausted = false;
        pageCache.clear();
        garudaRawPageCache.clear();
        sintaFilteredPool.clear();
        ojsArticlePool.clear();
        ojsIssueQueue.clear();
        masterArticleList.clear();
    }

    private Page cachedPage(int page) {
        List<Article> cached = pageCache.get(page);
        return cached == null ? null : new Page(cached, page, totalPages, totalRecords);
    }

    private List<Article> loadGarudaPage(GarudaSearchEngine engine, int page,
                                         Consumer<String> progress) throws Exception {
        int pageSize = activeRequest.pageSize();
        int startIndex = (page - 1) * pageSize;
        int endIndex = startIndex + pageSize;
        int portalStart = (startIndex / 10) + 1;
        int portalEnd = ((endIndex - 1) / 10) + 1;

        if (totalGarudaPages > 1) portalEnd = Math.min(portalEnd, totalGarudaPages);
        for (int portalPage = portalStart; portalPage <= portalEnd; portalPage++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!garudaRawPageCache.containsKey(portalPage)) {
                progress.accept("Mengambil halaman ke-" + portalPage + "...");
                GarudaSearchEngine.SearchOptions options = new GarudaSearchEngine.SearchOptions(
                        activeRequest.keyword(), "ALL", "ALL",
                        activeRequest.fromYear(), activeRequest.toYear(), portalPage);
                GarudaSearchEngine.PageResult result = engine.searchPage(options, catalog);
                garudaRawPageCache.put(portalPage, result);
                totalGarudaPages = Math.max(1, result.totalPages());
                totalGarudaRecords = result.totalRecords();
                totalRecords = totalGarudaRecords;
                totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize));
                if (portalPage == portalStart) {
                    portalEnd = Math.min(((endIndex - 1) / 10) + 1, totalGarudaPages);
                }
                if (!result.hasMore() || result.articles().isEmpty()) break;
            }
        }

        if (totalRecords > 0) endIndex = Math.min(endIndex, totalRecords);
        List<Article> result = new ArrayList<>();
        for (int portalPage = portalStart; portalPage <= portalEnd; portalPage++) {
            GarudaSearchEngine.PageResult pageResult = garudaRawPageCache.get(portalPage);
            if (pageResult == null) continue;
            int baseIndex = (portalPage - 1) * 10;
            for (int i = 0; i < pageResult.articles().size(); i++) {
                int globalIndex = baseIndex + i;
                if (globalIndex >= startIndex && globalIndex < endIndex) {
                    result.add(pageResult.articles().get(i));
                }
            }
        }
        return result;
    }

    private List<Article> loadSintaPage(GarudaSearchEngine engine, int page,
                                        Consumer<String> progress) throws Exception {
        int pageSize = activeRequest.pageSize();
        int targetQuota = page * pageSize;
        while (!sintaPoolExhausted && sintaFilteredPool.size() < targetQuota) {
            if (Thread.currentThread().isInterrupted()) break;
            int nextPage = sintaLastGarudaPageScanned + 1;
            if (totalGarudaPages > 1 && nextPage > totalGarudaPages) break;

            progress.accept("Memindai hal-" + nextPage + " ("
                    + sintaFilteredPool.size() + " artikel terkumpul)...");
            GarudaSearchEngine.PageResult result = garudaRawPageCache.get(nextPage);
            if (result == null) {
                GarudaSearchEngine.SearchOptions options = new GarudaSearchEngine.SearchOptions(
                        activeRequest.keyword(), "ALL", activeRequest.sintaRank(),
                        activeRequest.fromYear(), activeRequest.toYear(), nextPage);
                result = engine.searchPage(options, catalog);
                garudaRawPageCache.put(nextPage, result);
            }

            sintaLastGarudaPageScanned = nextPage;
            totalGarudaPages = Math.max(1, result.totalPages());
            totalGarudaRecords = result.totalRecords();
            sintaFilteredPool.addAll(result.articles());
            if (!result.hasMore() || result.articles().isEmpty()) {
                sintaPoolExhausted = true;
                break;
            }
        }

        if (sintaPoolExhausted || sintaLastGarudaPageScanned >= totalGarudaPages) {
            totalRecords = sintaFilteredPool.size();
            totalPages = Math.max(page, Math.max(1,
                    (int) Math.ceil((double) totalRecords / pageSize)));
        } else {
            double ratio = (double) sintaFilteredPool.size()
                    / Math.max(1, sintaLastGarudaPageScanned * 10);
            totalRecords = Math.max(sintaFilteredPool.size(), (int) (totalGarudaRecords * ratio));
            totalPages = Math.max(page, (int) Math.ceil((double) totalRecords / pageSize));
        }

        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(sintaFilteredPool.size(), startIndex + pageSize);
        return startIndex < sintaFilteredPool.size()
                ? new ArrayList<>(sintaFilteredPool.subList(startIndex, endIndex))
                : List.of();
    }

    private void runOjsLoad(int page, CrawlSource source) {
        long requestGeneration = generation;
        listener.onStarted("Menjelajah arsip OJS ...");
        OjsCrawler crawler = new OjsCrawler(source.baseUrl());
        currentWorker = new SwingWorker<>() {
            @Override
            protected List<Article> doInBackground() {
                if (ojsIssueQueue.isEmpty() && !ojsExhausted) {
                    publish("Membaca daftar edisi jurnal ...");
                    ojsIssueQueue.addAll(crawler.listIssues());
                    if (ojsIssueQueue.isEmpty()) ojsExhausted = true;
                }

                int targetQuota = page * activeRequest.pageSize();
                while (!ojsExhausted && ojsArticlePool.size() < targetQuota
                        && ojsIssuesScanned < ojsIssueQueue.size()) {
                    if (Thread.currentThread().isInterrupted()) break;
                    String issueUrl = ojsIssueQueue.get(ojsIssuesScanned++);
                    publish("Memindai edisi " + ojsIssuesScanned
                            + " dari " + ojsIssueQueue.size() + " ...");

                    for (Article raw : crawler.parseIssue(issueUrl)) {
                        if (Thread.currentThread().isInterrupted()) break;
                        Article enriched = crawler.enrich(raw);
                        if (!keywordMatches(activeRequest.keyword(), enriched)) continue;
                        if (!yearPasses(enriched.year())) continue;
                        if (catalog != null && !"ALL".equalsIgnoreCase(activeRequest.sintaRank())) {
                            String rank = catalog.resolveSintaRank(enriched.url(), enriched.journal());
                            if (!JournalCatalog.matchesSinta(rank, activeRequest.sintaRank())) continue;
                        }
                        if (!articleExists(ojsArticlePool, enriched)) ojsArticlePool.add(enriched);
                    }
                }

                ojsExhausted = ojsIssuesScanned >= ojsIssueQueue.size();
                totalRecords = ojsArticlePool.size();
                totalPages = Math.max(page, Math.max(1,
                        (int) Math.ceil((double) totalRecords / activeRequest.pageSize())));
                int startIndex = (page - 1) * activeRequest.pageSize();
                int endIndex = Math.min(ojsArticlePool.size(), startIndex + activeRequest.pageSize());
                return startIndex < ojsArticlePool.size()
                        ? new ArrayList<>(ojsArticlePool.subList(startIndex, endIndex))
                        : List.of();
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty() && requestGeneration == generation) {
                    listener.onProgress(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                if (requestGeneration == generation) finishPageLoad(page, this, source);
            }
        };
        currentWorker.execute();
    }

    private void finishPageLoad(int page, SwingWorker<List<Article>, String> worker, CrawlSource source) {
        listener.onFinished();
        if (worker.isCancelled()) {
            listener.onCancelled();
            return;
        }

        try {
            List<Article> articles = worker.get();
            currentPage = page;
            pageCache.put(page, articles);
            for (Article article : articles) {
                if (!articleExists(masterArticleList, article)) masterArticleList.add(article);
            }
            listener.onPageLoaded(new Page(articles, currentPage, totalPages, totalRecords));
        } catch (Exception ex) {
            String message = String.valueOf(ex.getMessage());
            if (source != null && message.contains("404")
                    && CrawlSource.TYPE_GARUDA.equalsIgnoreCase(source.type())
                    && !GarudaSearchEngine.DEFAULT_BASE_URL.equalsIgnoreCase(source.baseUrl())) {
                switchSourceToOjs(source);
                listener.onSourceFallback("Portal bukan Garuda — dialihkan otomatis ke mode OJS ...");
                runOjsLoad(page, source);
                return;
            }
            listener.onFailure("Gagal memuat Halaman " + page + ": " + message);
        }
    }

    private void switchSourceToOjs(CrawlSource source) {
        int index = crawlSources.indexOf(source);
        if (index < 0) return;
        crawlSources.set(index, new CrawlSource(source.name(), source.baseUrl(),
                CrawlSource.TYPE_OJS, source.enabled(), source.builtin()));
        try {
            CrawlSourceStore.save(sourceStoreFile, new ArrayList<>(crawlSources));
        } catch (Exception ignored) {
            // Gagal menyimpan tipe sumber tidak boleh menggagalkan hasil crawl.
        }
    }

    private CrawlSource activeSource() {
        return crawlSources.stream().filter(CrawlSource::enabled).findFirst().orElse(null);
    }

    private boolean keywordMatches(String keyword, Article article) {
        String normalized = keyword.toLowerCase().strip();
        if (normalized.isEmpty()) return true;
        return (article.title() != null && article.title().toLowerCase().contains(normalized))
                || (article.authors() != null && article.authors().toLowerCase().contains(normalized))
                || (article.abstractText() != null && article.abstractText().toLowerCase().contains(normalized));
    }

    private boolean yearPasses(String year) {
        if (year == null || year.length() < 4) return true;
        try {
            int value = Integer.parseInt(year.substring(0, 4));
            if (activeRequest.fromYear() != null && value < activeRequest.fromYear()) return false;
            if (activeRequest.toYear() != null && value > activeRequest.toYear()) return false;
            return true;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private static boolean articleExists(List<Article> articles, Article article) {
        return articles.stream().anyMatch(saved -> WorkspaceController.sameArticle(saved, article));
    }
}
