package id.gocrawl.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jendela Utama GoCrawl Desktop GUI:
 * Mengusung tata letak 2-panel bersih (Tabel Hasil Penelusuran & Detail Inspector Metadata),
 * terintegrasi langsung dengan mesin pencarian Portal Garuda, kontrol paginasi per-halaman,
 * dialog direktori jurnal terindeks, dan penyaring akreditasi SINTA.
 */
public class GoCrawlGui extends JFrame {

    public record SintaFilterItem(String id, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private final JTextField keywordField = new JTextField(20);
    private final JComboBox<SintaFilterItem> sintaComboBox = new JComboBox<>();
    private final JTextField fromYearField = new JTextField("2021", 5);
    private final JTextField toYearField = new JTextField("2026", 5);
    private final JButton crawlBtn = new JButton("Mulai Crawl");

    private JournalCatalog catalog;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;

    private SwingWorker<List<Article>, String> currentSearchWorker;
    private final List<Article> masterArticleList = new ArrayList<>();
    private final Map<Integer, List<Article>> pageCache = new ConcurrentHashMap<>();
    private final Map<Integer, GarudaSearchEngine.PageResult> garudaRawPageCache = new ConcurrentHashMap<>();
    private final List<Article> sintaFilteredPool = Collections.synchronizedList(new ArrayList<>());
    private int sintaLastGarudaPageScanned = 0;
    private final GarudaSearchEngine searchEngine = new GarudaSearchEngine();

    // State Paginasi Pencarian Garuda
    private int activePageSize = 25; // Default 25 baris untuk mengisi penuh ruang vertikal tabel
    private int currentSearchPage = 1;
    private int totalSearchPages = 1;
    private int totalRecordsFound = 0;
    private int totalGarudaPages = 1;
    private int totalGarudaRecords = 0;
    private String activeKeyword = "";
    private String activeSintaRank = "ALL";
    private Integer activeFromYear = null;
    private Integer activeToYear = null;

    public GoCrawlGui() {
        super("GoCrawl Desktop — Academic Research Engine");
        initCatalog();
        initUI();
    }

    private void initCatalog() {
        try {
            Path masterFile = Path.of("data", "journals_master.json");
            this.catalog = JournalCatalog.load(masterFile);
        } catch (Exception ex) {
            this.catalog = new JournalCatalog();
        }
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 780);
        setMinimumSize(new Dimension(980, 580));
        setLocationRelativeTo(null);

        // 1. Menu Bar Desktop
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem viewJournalsFileItem = new JMenuItem("Lihat Jurnal Terindeks...");
        viewJournalsFileItem.addActionListener(e -> showIndexedJournals());

        JMenuItem addJournalItem = new JMenuItem("Tambah Jurnal Sumber...");
        addJournalItem.addActionListener(e -> new AddJournalDialog(this, () -> {
            initCatalog();
            if (tablePanel != null) tablePanel.repaint();
        }).setVisible(true));

        JMenuItem exitItem = new JMenuItem("Keluar");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(viewJournalsFileItem);
        fileMenu.add(addJournalItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu catalogMenu = new JMenu("Katalog");
        JMenuItem viewJournalsItem = new JMenuItem("Lihat Jurnal Terindeks...");
        viewJournalsItem.addActionListener(e -> showIndexedJournals());
        catalogMenu.add(viewJournalsItem);
        menuBar.add(catalogMenu);

        setJMenuBar(menuBar);

        // 2. Toolbar Atas
        JPanel headerToolbar = new JPanel(new BorderLayout(10, 0));
        headerToolbar.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Judul Aplikasi
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel titleLabel = new JLabel("GoCrawl");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        brandBox.add(titleLabel);

        headerToolbar.add(brandBox, BorderLayout.WEST);

        // Inisialisasi ComboBox Akreditasi
        sintaComboBox.addItem(new SintaFilterItem("ALL", "Semua"));
        sintaComboBox.addItem(new SintaFilterItem("S1", "Minimal S1"));
        sintaComboBox.addItem(new SintaFilterItem("S2", "Minimal S2"));
        sintaComboBox.addItem(new SintaFilterItem("S3", "Minimal S3"));
        sintaComboBox.addItem(new SintaFilterItem("S4", "Minimal S4"));
        sintaComboBox.addItem(new SintaFilterItem("S5", "Minimal S5"));
        sintaComboBox.addItem(new SintaFilterItem("S6", "Minimal S6"));
        sintaComboBox.setPreferredSize(new Dimension(115, 28));
        sintaComboBox.setToolTipText("Filter batas minimal peringkat akreditasi jurnal");

        // Kontrol Pencarian di Tengah Toolbar
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        searchControls.add(new JLabel("Kata Kunci:"));
        searchControls.add(keywordField);

        searchControls.add(new JLabel("Akreditasi:"));
        searchControls.add(sintaComboBox);

        searchControls.add(new JLabel("Tahun:"));
        searchControls.add(fromYearField);
        searchControls.add(new JLabel("-"));
        searchControls.add(toYearField);

        // Tombol Mulai Crawl / ■ Stop
        crawlBtn.setFont(crawlBtn.getFont().deriveFont(Font.BOLD));
        crawlBtn.setPreferredSize(new Dimension(120, 30));
        searchControls.add(crawlBtn);

        headerToolbar.add(searchControls, BorderLayout.CENTER);
        add(headerToolbar, BorderLayout.NORTH);

        // 3. Tata Letak Bersih 2-Panel (Tabel Artikel di Kiri/Tengah & Detail Inspector di Kanan)
        inspectorPanel = new DetailInspectorPanel(catalog);
        tablePanel = new ArticleTablePanel(catalog, inspectorPanel::setArticle);
        tablePanel.setOnPageChange(this::loadPage);
        tablePanel.setOnPageSizeChange(newSize -> {
            if (newSize == activePageSize) return;
            this.activePageSize = newSize;
            this.pageCache.clear();
            if (totalRecordsFound > 0) {
                this.totalSearchPages = Math.max(1, (int) Math.ceil((double) totalRecordsFound / activePageSize));
            }
            if (activeKeyword != null && !activeKeyword.isBlank()) {
                loadPage(1);
            }
        });

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        mainSplit.setResizeWeight(0.68);
        mainSplit.setDividerSize(6);

        add(mainSplit, BorderLayout.CENTER);

        // Pesan Awal
        tablePanel.setProgress(false, "Ketikkan kata kunci pencarian lalu tekan Enter atau klik 'Mulai Crawl'.");

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        keywordField.addActionListener(e -> handleCrawlToggle());
    }

    private void showIndexedJournals() {
        new IndexedJournalsDialog(this, catalog, () -> {
            initCatalog();
            if (tablePanel != null) tablePanel.repaint();
        }).setVisible(true);
    }

    private void handleCrawlToggle() {
        if (currentSearchWorker != null && !currentSearchWorker.isDone()) {
            currentSearchWorker.cancel(true);
            crawlBtn.setText("Mulai Crawl");
            crawlBtn.setForeground(null);
            tablePanel.setProgress(false, "Penelusuran dihentikan oleh pengguna.");
            tablePanel.setPaginationEnabled(true);
            return;
        }

        String keyword = keywordField.getText().strip();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ketikkan kata kunci pencarian terlebih dahulu (cth: 'pendidikan karakter', 'deep learning').",
                    "Validasi Kata Kunci",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer fromYear = null;
        Integer toYear = null;
        try {
            if (!fromYearField.getText().isBlank()) fromYear = Integer.parseInt(fromYearField.getText().strip());
            if (!toYearField.getText().isBlank()) toYear = Integer.parseInt(toYearField.getText().strip());
            if (fromYear != null && toYear != null && fromYear > toYear) {
                JOptionPane.showMessageDialog(this, "Tahun awal tidak boleh melebihi tahun akhir.", "Validasi Tahun", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format tahun harus berupa angka.", "Validasi Tahun", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SintaFilterItem selectedSinta = (SintaFilterItem) sintaComboBox.getSelectedItem();

        // Simpan parameter aktif untuk paginasi berikutnya
        this.activePageSize = tablePanel.getPageSize();
        this.activeKeyword = keyword;
        this.activeSintaRank = selectedSinta != null ? selectedSinta.id() : "ALL";
        this.activeFromYear = fromYear;
        this.activeToYear = toYear;
        this.currentSearchPage = 1;
        this.totalSearchPages = 1;
        this.totalRecordsFound = 0;
        this.totalGarudaPages = 1;
        this.totalGarudaRecords = 0;
        this.sintaLastGarudaPageScanned = 0;
        this.sintaFilteredPool.clear();

        // Reset cache dan tabel untuk pencarian baru
        garudaRawPageCache.clear();
        pageCache.clear();
        masterArticleList.clear();
        tablePanel.clearArticles();

        loadPage(1);
    }

    private void loadPage(int page) {
        if (currentSearchWorker != null && !currentSearchWorker.isDone()) {
            currentSearchWorker.cancel(true);
        }

        if (activeKeyword == null || activeKeyword.isBlank()) {
            activeKeyword = keywordField.getText().strip();
        }
        if (activeKeyword.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ketikkan kata kunci pencarian terlebih dahulu.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Navigasi Instan jika halaman sudah tersimpan di cache
        if (pageCache.containsKey(page)) {
            currentSearchPage = page;
            List<Article> cached = pageCache.get(page);
            tablePanel.setArticles(cached);
            tablePanel.updatePagination(page, totalSearchPages, totalRecordsFound, cached.size());
            tablePanel.setProgress(false, "Halaman " + page + " dari " + totalSearchPages + " — Menampilkan " + cached.size() + " artikel");
            return;
        }

        crawlBtn.setText("■ Stop");
        crawlBtn.setForeground(Color.RED);
        tablePanel.setPaginationEnabled(false);
        tablePanel.setProgress(true, "Memuat Halaman " + page + (totalSearchPages > 1 ? " dari " + totalSearchPages : "") + " (" + activePageSize + " artikel/hal)...");

        currentSearchWorker = new SwingWorker<List<Article>, String>() {
            @Override
            protected List<Article> doInBackground() throws Exception {
                boolean isAllSinta = "ALL".equalsIgnoreCase(activeSintaRank);

                if (isAllSinta) {
                    // Mode Standar (Semua SINTA): Peta indeks ke halaman Portal Garuda (10 item per halaman portal)
                    int startIdx = (page - 1) * activePageSize;
                    int endIdx = startIdx + activePageSize;

                    int gStart = (startIdx / 10) + 1;
                    int gEnd = ((endIdx - 1) / 10) + 1;
                    if (totalGarudaPages > 1) {
                        gEnd = Math.min(gEnd, totalGarudaPages);
                    }

                    for (int g = gStart; g <= gEnd; g++) {
                        if (isCancelled()) break;
                        if (!garudaRawPageCache.containsKey(g)) {
                            publish("Mengambil halaman ke-" + g + "...");
                            GarudaSearchEngine.SearchOptions opts = new GarudaSearchEngine.SearchOptions(
                                    activeKeyword,
                                    "ALL",
                                    "ALL",
                                    activeFromYear,
                                    activeToYear,
                                    g
                            );
                            GarudaSearchEngine.PageResult res = searchEngine.searchPage(opts, catalog);
                            garudaRawPageCache.put(g, res);
                            totalGarudaPages = Math.max(1, res.totalPages());
                            totalGarudaRecords = res.totalRecords();
                            totalRecordsFound = totalGarudaRecords;
                            totalSearchPages = Math.max(1, (int) Math.ceil((double) totalRecordsFound / activePageSize));
                            if (g == gStart) {
                                gEnd = Math.min(((endIdx - 1) / 10) + 1, totalGarudaPages);
                            }
                            if (!res.hasMore() || res.articles().isEmpty()) {
                                break;
                            }
                        }
                    }

                    // Kumpulkan artikel untuk rentang halaman UI ini
                    List<Article> resultList = new ArrayList<>();
                    if (totalRecordsFound > 0) {
                        endIdx = Math.min(endIdx, totalRecordsFound);
                    }
                    for (int g = gStart; g <= gEnd; g++) {
                        GarudaSearchEngine.PageResult gRes = garudaRawPageCache.get(g);
                        if (gRes == null) continue;
                        List<Article> gList = gRes.articles();
                        int gBase = (g - 1) * 10;
                        for (int i = 0; i < gList.size(); i++) {
                            int globalIdx = gBase + i;
                            if (globalIdx >= startIdx && globalIdx < endIdx) {
                                resultList.add(gList.get(i));
                            }
                        }
                    }
                    return resultList;
                } else {
                    // Mode Filter SINTA: Pindai halaman Garuda secara dinamis hingga kuota per-halaman terpenuhi
                    int targetQuota = page * activePageSize;

                    while (sintaFilteredPool.size() < targetQuota) {
                        if (isCancelled()) break;
                        int nextG = sintaLastGarudaPageScanned + 1;
                        if (totalGarudaPages > 1 && nextG > totalGarudaPages) {
                            break;
                        }

                        publish("Memindai hal-" + nextG + " (" + sintaFilteredPool.size() + " artikel terkumpul)...");

                        GarudaSearchEngine.PageResult res;
                        if (garudaRawPageCache.containsKey(nextG)) {
                            res = garudaRawPageCache.get(nextG);
                        } else {
                            GarudaSearchEngine.SearchOptions opts = new GarudaSearchEngine.SearchOptions(
                                    activeKeyword,
                                    "ALL",
                                    activeSintaRank,
                                    activeFromYear,
                                    activeToYear,
                                    nextG
                            );
                            res = searchEngine.searchPage(opts, catalog);
                            garudaRawPageCache.put(nextG, res);
                        }

                        sintaLastGarudaPageScanned = nextG;
                        totalGarudaPages = Math.max(1, res.totalPages());
                        totalGarudaRecords = res.totalRecords();
                        sintaFilteredPool.addAll(res.articles());

                        if (!res.hasMore() || (res.articles().isEmpty() && nextG >= totalGarudaPages)) {
                            break;
                        }
                    }

                    if (sintaLastGarudaPageScanned >= totalGarudaPages) {
                        totalRecordsFound = sintaFilteredPool.size();
                        totalSearchPages = Math.max(1, (int) Math.ceil((double) totalRecordsFound / activePageSize));
                    } else {
                        double ratio = (double) sintaFilteredPool.size() / Math.max(1, sintaLastGarudaPageScanned * 10);
                        totalRecordsFound = Math.max(sintaFilteredPool.size(), (int) (totalGarudaRecords * ratio));
                        totalSearchPages = Math.max(page, (int) Math.ceil((double) totalRecordsFound / activePageSize));
                    }

                    int startIdx = (page - 1) * activePageSize;
                    int endIdx = Math.min(sintaFilteredPool.size(), startIdx + activePageSize);
                    if (startIdx < sintaFilteredPool.size()) {
                        return new ArrayList<>(sintaFilteredPool.subList(startIdx, endIdx));
                    } else {
                        return Collections.emptyList();
                    }
                }
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    tablePanel.setProgress(true, chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                crawlBtn.setText("Mulai Crawl");
                crawlBtn.setForeground(null);
                tablePanel.setPaginationEnabled(true);

                if (isCancelled()) {
                    tablePanel.setProgress(false, "Pemuatan halaman dihentikan.");
                    return;
                }

                try {
                    List<Article> articles = get();
                    currentSearchPage = page;
                    pageCache.put(page, articles);

                    // Kumpulkan ke master list untuk ekspor bibliografi (deduplikasi berdasarkan URL/DOI)
                    for (Article a : articles) {
                        boolean exists = masterArticleList.stream().anyMatch(m ->
                                (!m.url().isBlank() && m.url().equalsIgnoreCase(a.url())) ||
                                (!m.doi().isBlank() && m.doi().equalsIgnoreCase(a.doi())) ||
                                m.title().equalsIgnoreCase(a.title()));
                        if (!exists) {
                            masterArticleList.add(a);
                        }
                    }

                    tablePanel.setArticles(articles);
                    tablePanel.updatePagination(currentSearchPage, totalSearchPages, totalRecordsFound, articles.size());
                    tablePanel.setProgress(false, "Halaman " + currentSearchPage + " dari " + totalSearchPages +
                            " — Menampilkan " + articles.size() + " artikel (Total: " + totalRecordsFound + " dokumen)");

                    saveSearchOutput();
                } catch (Exception e) {
                    tablePanel.setProgress(false, "Gagal memuat Halaman " + page + ": " + e.getMessage());
                }
            }
        };

        currentSearchWorker.execute();
    }

    private void saveSearchOutput() {
        if (masterArticleList.isEmpty()) return;
        try {
            Path outDir = Path.of("output", "garuda_search");
            Files.createDirectories(outDir);
            ArticleStorage.saveJson(outDir.resolve("articles.json"), masterArticleList);
        } catch (Exception ignored) {}
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception ignored) {}
            new GoCrawlGui().setVisible(true);
        });
    }
}
