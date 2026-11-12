package id.gocrawl.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Jendela Utama GoCrawl Desktop GUI:
 * Menggabungkan arsitektur 3-panel ala Mendeley (Sidebar, Tabel Artikel, dan Detail Metadata),
 * Toolbar dengan morphing button [Mulai Crawl / ■], dan saklar tema gelap/terang FlatLaf.
 */
public class GoCrawlGui extends JFrame {

    private final JTextField keywordField = new JTextField(18);
    private final JTextField fromYearField = new JTextField("2021", 5);
    private final JTextField toYearField = new JTextField("2026", 5);
    private final JButton crawlBtn = new JButton("Mulai Crawl");
    private final JButton themeBtn = new JButton("Tema: Gelap");

    private JournalCatalog catalog;
    private SidebarPanel sidebarPanel;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;

    private boolean isDarkMode = false;
    private SwingWorker<Void, Article> currentCrawlWorker;
    private final List<Article> masterArticleList = new ArrayList<>();

    public GoCrawlGui() {
        super("GoCrawl Desktop (Academic Research Crawler)");
        initCatalog();
        initUI();
        loadWorkspaceHistory();
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

        // 1. Toolbar Atas
        JPanel headerToolbar = new JPanel(new BorderLayout(10, 0));
        headerToolbar.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Judul Aplikasi
        JLabel titleLabel = new JLabel("GoCrawl");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerToolbar.add(titleLabel, BorderLayout.WEST);

        // Kontrol Pencarian di Tengah Toolbar
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        searchControls.add(new JLabel("Kata Kunci:"));
        searchControls.add(keywordField);

        searchControls.add(new JLabel("Tahun:"));
        searchControls.add(fromYearField);
        searchControls.add(new JLabel("-"));
        searchControls.add(toYearField);

        // Morphing Crawl/Stop Button
        crawlBtn.setFont(crawlBtn.getFont().deriveFont(Font.BOLD));
        crawlBtn.setPreferredSize(new Dimension(120, 30));
        searchControls.add(crawlBtn);

        headerToolbar.add(searchControls, BorderLayout.CENTER);

        // Saklar Tema di Kanan Toolbar
        headerToolbar.add(themeBtn, BorderLayout.EAST);
        add(headerToolbar, BorderLayout.NORTH);

        // 2. Tiga Panel Utama (Sidebar | Tabel Artikel | Detail Inspector)
        inspectorPanel = new DetailInspectorPanel(catalog);
        tablePanel = new ArticleTablePanel(catalog, inspectorPanel::setArticle);
        sidebarPanel = new SidebarPanel(catalog, this::handleSidebarFilter);

        // Split Tengah & Kanan
        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        centerRightSplit.setResizeWeight(0.66);
        centerRightSplit.setDividerSize(6);

        // Split Kiri & Kanan Utama
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, centerRightSplit);
        mainSplit.setResizeWeight(0.20);
        mainSplit.setDividerSize(6);

        add(mainSplit, BorderLayout.CENTER);

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        themeBtn.addActionListener(e -> toggleTheme());
    }

    private void handleCrawlToggle() {
        if (currentCrawlWorker != null && !currentCrawlWorker.isDone()) {
            // Sedang aktif -> Hentikan (Stop)
            currentCrawlWorker.cancel(true);
            crawlBtn.setText("Mulai Crawl");
            crawlBtn.setForeground(null);
            tablePanel.setProgress(false, "Penelusuran dihentikan oleh pengguna.");
            return;
        }

        // Mulai Crawl Baru
        String keyword = keywordField.getText().strip();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ketikkan kata kunci pencarian terlebih dahulu (mendukung operator koma atau OR).",
                    "Validasi Kueri",
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

        // Kumpulkan target jurnal: dari catalog dan journal.txt
        List<URI> targets = new ArrayList<>();
        try {
            targets.addAll(Main.readTargets(Path.of("journal.txt")));
        } catch (Exception ignored) {}

        // Tambahkan juga target OJS dari katalog lokal
        if (catalog != null) {
            for (Journal j : catalog.allJournals()) {
                if (j.ojsUrl() != null && !j.ojsUrl().isBlank()) {
                    try {
                        URI u = URI.create(j.ojsUrl());
                        if (!targets.contains(u)) targets.add(u);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Belum ada jurnal target yang terindeks. Tambahkan jurnal melalui tombol '+ Tambah Jurnal Sumber'.",
                    "Target Jurnal Kosong",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Ubah tombol menjadi kotak Stop [■]
        crawlBtn.setText("■ Stop");
        crawlBtn.setForeground(Color.RED);
        tablePanel.setProgress(true, "Mengindeks & menghubungi portal jurnal...");

        final Integer fYear = fromYear;
        final Integer tYear = toYear;
        final Main.Options options = new Main.Options(keyword, fYear, tYear, Path.of("output"), false, false, false, null, false, null);

        currentCrawlWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                int count = 0;
                for (URI target : targets) {
                    if (isCancelled()) break;
                    count++;
                    String jName = Main.journalName(target);
                    String origin = catalog != null ? catalog.resolveOrigin(target.toString(), jName) : target.getHost();
                    tablePanel.setProgress(true, "Menelusuri [" + count + "/" + targets.size() + "]: " + jName + " (" + origin + ")");
                    Path folder = Path.of("output", jName);
                    Files.createDirectories(folder);

                    List<Article> batch = new ArrayList<>();
                    Main.ArticleSink sink = article -> {
                        if (isCancelled()) return;
                        publish(article);
                        synchronized (batch) {
                            batch.add(article);
                            try {
                                ArticleStorage.saveJson(folder.resolve("articles.json"), batch);
                            } catch (Exception ignored) {}
                        }
                    };

                    try {
                        Main.crawlTarget(target, options, sink);
                    } catch (Exception e) {
                        System.err.println("[GUI CRAWL GAGAL] " + jName + ": " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Article> chunks) {
                for (Article a : chunks) {
                    masterArticleList.add(a);
                    tablePanel.addArticle(a);
                }
            }

            @Override
            protected void done() {
                crawlBtn.setText("Mulai Crawl");
                crawlBtn.setForeground(null);
                if (isCancelled()) {
                    tablePanel.setProgress(false, "Penelusuran dibatalkan.");
                } else {
                    tablePanel.setProgress(false, "Penelusuran selesai.");
                    JOptionPane.showMessageDialog(GoCrawlGui.this,
                            "Penelusuran selesai! Total artikel terkumpul: " + tablePanel.getAllArticles().size(),
                            "Crawling Selesai",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };

        currentCrawlWorker.execute();
    }

    private void handleSidebarFilter(String filter) {
        if (filter.equals("ALL")) {
            tablePanel.setArticles(masterArticleList);
        } else if (filter.startsWith("SUBJECT:")) {
            String subjectName = filter.substring("SUBJECT:".length()).toLowerCase();
            List<Article> matched = masterArticleList.stream()
                    .filter(a -> a.keywords().toLowerCase().contains(subjectName)
                            || a.journal().toLowerCase().contains(subjectName)
                            || a.title().toLowerCase().contains(subjectName))
                    .toList();
            tablePanel.setArticles(matched);
        } else if (filter.startsWith("JOURNAL:")) {
            String targetKey = filter.substring("JOURNAL:".length()).toLowerCase();
            List<Article> matched = masterArticleList.stream()
                    .filter(a -> a.url().toLowerCase().contains(targetKey)
                            || a.journal().toLowerCase().contains(targetKey))
                    .toList();
            tablePanel.setArticles(matched);
        }
    }

    private void loadWorkspaceHistory() {
        SwingWorker<List<Article>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Article> doInBackground() {
                return ArticleStorage.scanWorkspace(Path.of("output"));
            }

            @Override
            protected void done() {
                try {
                    List<Article> loaded = get();
                    if (!loaded.isEmpty()) {
                        masterArticleList.clear();
                        masterArticleList.addAll(loaded);
                        tablePanel.setArticles(loaded);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
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
