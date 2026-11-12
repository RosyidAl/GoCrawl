package id.gocrawl.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private final JButton themeBtn = new JButton("Tema: Gelap");
    private final JButton viewCatalogBtn = new JButton("📚 Jurnal Terindeks");

    private JournalCatalog catalog;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;

    private boolean isDarkMode = false;
    private SwingWorker<Void, Article> currentSearchWorker;
    private final List<Article> masterArticleList = new ArrayList<>();
    private final Map<Integer, List<Article>> pageCache = new ConcurrentHashMap<>();
    private final GarudaSearchEngine searchEngine = new GarudaSearchEngine();

    // State Paginasi Pencarian Garuda
    private int currentSearchPage = 1;
    private int totalSearchPages = 1;
    private int totalRecordsFound = 0;
    private String activeKeyword = "";
    private String activeSintaRank = "ALL";
    private Integer activeFromYear = null;
    private Integer activeToYear = null;

    public GoCrawlGui() {
        super("GoCrawl Desktop — Academic Research Engine (Garuda & SINTA)");
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
        JMenuItem addJournalItem = new JMenuItem("Tambah Jurnal Sumber...");
        addJournalItem.addActionListener(e -> new AddJournalDialog(this, () -> {
            initCatalog();
            if (tablePanel != null) tablePanel.repaint();
        }).setVisible(true));

        JMenuItem exitItem = new JMenuItem("Keluar");
        exitItem.addActionListener(e -> System.exit(0));

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

        // Judul Aplikasi & Badge Agregator
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel titleLabel = new JLabel("GoCrawl");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        JLabel subBadge = new JLabel("Garuda & SINTA");
        subBadge.setFont(subBadge.getFont().deriveFont(Font.PLAIN, 10f));
        subBadge.setForeground(new Color(120, 120, 120));
        brandBox.add(titleLabel);
        brandBox.add(subBadge);

        viewCatalogBtn.setFont(viewCatalogBtn.getFont().deriveFont(Font.BOLD, 12f));
        viewCatalogBtn.setToolTipText("Buka direktori seluruh jurnal ilmiah yang telah terdaftar di Master Registry");
        viewCatalogBtn.addActionListener(e -> showIndexedJournals());
        brandBox.add(Box.createHorizontalStrut(6));
        brandBox.add(viewCatalogBtn);

        headerToolbar.add(brandBox, BorderLayout.WEST);

        // Inisialisasi ComboBox Akreditasi SINTA
        sintaComboBox.addItem(new SintaFilterItem("ALL", "Semua SINTA"));
        sintaComboBox.addItem(new SintaFilterItem("S1", "Minimal SINTA 1"));
        sintaComboBox.addItem(new SintaFilterItem("S2", "Minimal SINTA 2"));
        sintaComboBox.addItem(new SintaFilterItem("S3", "Minimal SINTA 3"));
        sintaComboBox.addItem(new SintaFilterItem("S4", "Minimal SINTA 4"));
        sintaComboBox.addItem(new SintaFilterItem("S5", "Minimal SINTA 5"));
        sintaComboBox.addItem(new SintaFilterItem("S6", "Minimal SINTA 6"));
        sintaComboBox.setPreferredSize(new Dimension(140, 28));
        sintaComboBox.setToolTipText("Filter batas minimal peringkat akreditasi jurnal nasional");

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

        // Saklar Tema di Kanan Toolbar
        headerToolbar.add(themeBtn, BorderLayout.EAST);
        add(headerToolbar, BorderLayout.NORTH);

        // 3. Tata Letak Bersih 2-Panel (Tabel Artikel di Kiri/Tengah & Detail Inspector di Kanan)
        inspectorPanel = new DetailInspectorPanel(catalog);
        tablePanel = new ArticleTablePanel(catalog, inspectorPanel::setArticle);
        tablePanel.setOnPageChange(this::loadPage);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        mainSplit.setResizeWeight(0.68);
        mainSplit.setDividerSize(6);

        add(mainSplit, BorderLayout.CENTER);

        // Pesan Awal
        tablePanel.setProgress(false, "Ketikkan kata kunci pencarian lalu tekan Enter atau klik 'Mulai Crawl'.");

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        themeBtn.addActionListener(e -> toggleTheme());
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
        this.activeKeyword = keyword;
        this.activeSintaRank = selectedSinta != null ? selectedSinta.id() : "ALL";
        this.activeFromYear = fromYear;
        this.activeToYear = toYear;
        this.currentSearchPage = 1;
        this.totalSearchPages = 1;
        this.totalRecordsFound = 0;

        // Reset cache dan tabel untuk pencarian baru
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
        tablePanel.setProgress(true, "Memuat Halaman " + page + (totalSearchPages > 1 ? " dari " + totalSearchPages : "") + " dari Portal Garuda...");

        currentSearchWorker = new SwingWorker<>() {
            private GarudaSearchEngine.PageResult pageResult;

            @Override
            protected Void doInBackground() throws Exception {
                GarudaSearchEngine.SearchOptions opts = new GarudaSearchEngine.SearchOptions(
                        activeKeyword,
                        "ALL",
                        activeSintaRank,
                        activeFromYear,
                        activeToYear,
                        page
                );

                pageResult = searchEngine.searchPage(opts, catalog);
                return null;
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
                    get();
                    if (pageResult != null) {
                        currentSearchPage = pageResult.currentPage();
                        totalSearchPages = Math.max(1, pageResult.totalPages());
                        totalRecordsFound = pageResult.totalRecords();

                        List<Article> articles = pageResult.articles();
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
                                " — Menampilkan " + articles.size() + " artikel (Total: " + totalRecordsFound + " dokumen di Garuda)");

                        saveSearchOutput();
                    }
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

    private void toggleTheme() {
        try {
            if (isDarkMode) {
                FlatLightLaf.setup();
                themeBtn.setText("Tema: Gelap");
                isDarkMode = false;
            } else {
                FlatDarkLaf.setup();
                themeBtn.setText("Tema: Terang");
                isDarkMode = true;
            }
            FlatLaf.updateUI();
        } catch (Exception ex) {
            System.err.println("Gagal beralih tema: " + ex.getMessage());
        }
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
