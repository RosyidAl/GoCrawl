package id.gocrawl.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
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

    /** Warna tombol Cari (biru) dan tombol Stop (merah) pada baris pencarian. */
    private static final Color SEARCH_BTN_BLUE = new Color(0, 114, 188);
    private static final Color SEARCH_BTN_RED = new Color(198, 40, 40);

    /** Lebar kolom sidebar; sekaligus garis potong kiri bar pencarian (tepi kanan sidebar). */
    private static final int SIDEBAR_WIDTH = 220;

    private final JTextField keywordField = new JTextField(20);
    private final JComboBox<SintaFilterItem> sintaComboBox = new JComboBox<>();
    private final JTextField fromYearField = new JTextField("2021", 5);
    private final JTextField toYearField = new JTextField("2026", 5);
    private final JButton crawlBtn = new JButton("🔍 Cari");
    private final JTextField advancedJournalField = new JTextField();
    private final JTextField advancedAuthorField = new JTextField();
    private final JTextField advancedAffiliationField = new JTextField();
    private JPanel advancedSearchPanel;
    private JLayeredPane contentLayer;
    private JPanel searchControls;
    private JPanel headerToolbar;
    private JSplitPane mainSplit;
    private JPanel sidebar;
    private final JLabel projectPathLabel = new JLabel("Default workspace");
    private Path activeProject = Path.of(".").toAbsolutePath().normalize();

    private JournalCatalog catalog;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;

    private SwingWorker<List<Article>, String> currentSearchWorker;
    private final List<Article> masterArticleList = new ArrayList<>();
    private final Map<Integer, List<Article>> pageCache = new ConcurrentHashMap<>();
    private final Map<Integer, GarudaSearchEngine.PageResult> garudaRawPageCache = new ConcurrentHashMap<>();
    private final List<Article> sintaFilteredPool = Collections.synchronizedList(new ArrayList<>());
    private int sintaLastGarudaPageScanned = 0;
    private boolean sintaPoolExhausted = false;
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
        setSize(1440, 820);
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

        JMenuItem saveResultsItem = new JMenuItem("Simpan Hasil Pencarian...");
        saveResultsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveResultsItem.addActionListener(e -> saveResultsManually());

        JMenuItem exitItem = new JMenuItem("Keluar");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(viewJournalsFileItem);
        fileMenu.add(addJournalItem);
        fileMenu.add(saveResultsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu catalogMenu = new JMenu("Katalog");
        JMenuItem viewJournalsItem = new JMenuItem("Lihat Jurnal Terindeks...");
        viewJournalsItem.addActionListener(e -> showIndexedJournals());
        catalogMenu.add(viewJournalsItem);
        menuBar.add(catalogMenu);

        setJMenuBar(menuBar);

        // 2. Toolbar Atas (baris pencarian saja — baris filter lanjutan diposisikan
        // terpisah di contentLayer agar sidebar tidak ikut turun)
        headerToolbar = new JPanel(new BorderLayout(10, 0));
        headerToolbar.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Judul Aplikasi — kolomnya dibuat selebar sidebar, sehingga bar pencarian
        // di potong (mulai) tepat di tepi kanan sidebar
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel titleLabel = new JLabel("GoCrawl");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        brandBox.add(titleLabel);
        // 14px = margin kiri headerToolbar, dikoreksi agar tepi kanan kolom brand
        // = tepi kanan sidebar (220 + 5 divider), sehingga bar pencarian mulai
        // persis di garis potong sidebar
        brandBox.setPreferredSize(new Dimension(SIDEBAR_WIDTH + 5 - 14, brandBox.getPreferredSize().height));

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

        // Baris pencarian gaya "Find articles": tiga kolom input berlabel di atasnya,
        // tanpa kotak biru pembungkus, tombol Cari biru, dan tautan Pencarian Lanjutan
        crawlBtn.setText("🔍 Cari");
        crawlBtn.setFont(crawlBtn.getFont().deriveFont(Font.BOLD, 12f));
        crawlBtn.setBackground(SEARCH_BTN_BLUE);
        crawlBtn.setForeground(Color.WHITE);
        crawlBtn.setPreferredSize(new Dimension(95, 30));

        advancedSearchPanel = createAdvancedSearchPanel();
        advancedSearchPanel.setVisible(false);
        JButton advancedSearchButton = new JButton("Pencarian Lanjutan ▾");
        advancedSearchButton.putClientProperty("JButton.buttonType", "toolBarButton");
        advancedSearchButton.addActionListener(e -> toggleAdvancedSearch(advancedSearchButton));

        // Esc menutup baris filter lanjutan (no-op saat memang sudah tertutup)
        advancedSearchPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "tutupPencarianLanjutan");
        advancedSearchPanel.getActionMap().put("tutupPencarianLanjutan", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent evt) {
                if (advancedSearchPanel.isVisible()) {
                    toggleAdvancedSearch(advancedSearchButton);
                }
            }
        });

        searchControls = createSearchBar(advancedSearchButton);

        // Wrapper FlowLayout kanan: baris pencarian digeser mentok ke tepi kanan jendela
        JPanel searchArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchArea.setOpaque(false);
        searchArea.add(searchControls);
        headerToolbar.add(searchArea, BorderLayout.CENTER);
        

        // 3. Tata Letak Bersih 2-Panel (Tabel Artikel di Kiri/Tengah & Detail Inspector di Kanan)
        inspectorPanel = new DetailInspectorPanel(catalog);
        // Inspector disembunyikan di awal; muncul hanya saat artikel dipilih di tabel
        inspectorPanel.setVisible(false);
        tablePanel = new ArticleTablePanel(catalog, this::showArticleInInspector);
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

        // Sidebar dan area konten diposisikan langsung di contentLayer (tanpa appSplit):
        // saat baris filter lanjutan terbuka, hanya area konten tengah yang turun —
        // sidebar tetap menempel di bawah bar pencarian
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        mainSplit.setResizeWeight(0.68);
        mainSplit.setDividerSize(6);

        sidebar = createSidebar();

        contentLayer = new JLayeredPane();
        contentLayer.setLayout(null);
        contentLayer.add(headerToolbar, JLayeredPane.DEFAULT_LAYER);
        contentLayer.add(sidebar, JLayeredPane.DEFAULT_LAYER);
        contentLayer.add(mainSplit, JLayeredPane.DEFAULT_LAYER);
        contentLayer.add(advancedSearchPanel, JLayeredPane.DEFAULT_LAYER);
        contentLayer.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutContentLayer();
            }
        });
        add(contentLayer, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::layoutContentLayer);

        // Pesan Awal
        tablePanel.setProgress(false, "Ketikkan kata kunci pencarian lalu tekan Enter atau klik 'Mulai Crawl'.");

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        keywordField.addActionListener(e -> handleCrawlToggle());
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setBorder(new EmptyBorder(18, 14, 14, 12));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 500));
        sidebar.setBackground(new Color(248, 249, 252));
        JPanel project = new JPanel();
        project.setOpaque(false);
        project.setLayout(new BoxLayout(project, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("PROYEK SAAT INI");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
        heading.setForeground(new Color(105, 112, 125));
        project.add(heading);
        project.add(Box.createVerticalStrut(8));
        projectPathLabel.setFont(projectPathLabel.getFont().deriveFont(Font.BOLD, 13f));
        projectPathLabel.setToolTipText(activeProject.toString());
        project.add(projectPathLabel);
        project.add(Box.createVerticalStrut(8));
        JButton manage = new JButton("Buka Folder Proyek…");
        manage.setAlignmentX(Component.LEFT_ALIGNMENT);
        manage.addActionListener(e -> chooseProjectFolder());
        project.add(manage);
        project.add(Box.createVerticalStrut(5));
        JButton create = new JButton("Buat Proyek Baru…");
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.addActionListener(e -> createProjectFolder());
        project.add(create);
        sidebar.add(project, BorderLayout.NORTH);
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        JLabel all = new JLabel("▤   Semua Artikel");
        all.setOpaque(true);
        all.setBackground(new Color(228, 235, 251));
        all.setForeground(new Color(48, 78, 158));
        all.setBorder(new EmptyBorder(10, 9, 10, 6));
        all.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(all);
        sidebar.add(nav, BorderLayout.CENTER);
        return sidebar;
    }

    private void chooseProjectFolder() {
        JFileChooser chooser = new JFileChooser(activeProject.toFile());
        chooser.setDialogTitle("Pilih folder proyek");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            activeProject = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            projectPathLabel.setText(activeProject.getFileName() == null ? activeProject.toString() : activeProject.getFileName().toString());
            projectPathLabel.setToolTipText(activeProject.toString());
        }
    }

    private void createProjectFolder() {
        String name = JOptionPane.showInputDialog(this, "Nama proyek baru:", "Buat Proyek", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        name = name.strip();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
            JOptionPane.showMessageDialog(this, "Masukkan nama folder proyek yang valid.", "Nama Proyek", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path projectsRoot = Path.of("projects").toAbsolutePath().normalize();
        Path newProject = projectsRoot.resolve(name).normalize();
        if (!newProject.getParent().equals(projectsRoot)) {
            JOptionPane.showMessageDialog(this, "Nama proyek tidak valid.", "Nama Proyek", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Files.createDirectories(newProject);
            activeProject = newProject;
            projectPathLabel.setText(name);
            projectPathLabel.setToolTipText(activeProject.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal membuat proyek: " + ex.getMessage(), "Proyek", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Bar pencarian gaya "Find articles": tiga kolom berlabel + tombol Cari + tautan lanjutan. */
    private JPanel createSearchBar(JButton advancedSearchButton) {
        JPanel bar = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 10);

        c.weightx = 0.36;
        bar.add(createLabeledSearchField("Cari artikel dengan kata kunci", keywordField), c);
        c.weightx = 0.36;
        bar.add(createLabeledSearchField("Dalam judul jurnal atau buku", advancedJournalField), c);
        c.weightx = 0.18;
        bar.add(createLabeledSearchField("Penulis", advancedAuthorField), c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        // Rata bawah sel agar tombol sejajar (level) dengan baris field, bukan center
        // terhadap label + field
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 0, 0, 4);
        bar.add(crawlBtn, c);
        bar.add(advancedSearchButton, c);
        return bar;
    }

    /** Pasangan label kecil di atas komponen input (meniru gaya ScienceDirect). */
    private JPanel createLabeledSearchField(String label, JComponent field) {
        JPanel box = new JPanel(new BorderLayout(0, 2));
        box.setOpaque(false);
        JLabel heading = new JLabel(label);
        heading.setFont(heading.getFont().deriveFont(Font.PLAIN, 11f));
        heading.setForeground(new Color(75, 75, 75));
        // Label direntangkan selebar field (BorderLayout.NORTH) dengan teks rata kiri,
        // sehingga selalu nempel tepat di tepi kiri field-nya
        heading.setHorizontalAlignment(JLabel.LEFT);
        box.add(heading, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    /** Baris filter lanjutan: satu baris kompak inline di bawah bar pencarian. */
    private JPanel createAdvancedSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panel.setOpaque(false); // tanpa kotak putih pembungkus

        advancedAffiliationField.setColumns(24);
        panel.add(createLabeledSearchField("Afiliasi", advancedAffiliationField));
        panel.add(createLabeledSearchField("Akreditasi", sintaComboBox));

        // Pasangan tahun awal-akhir dibungkus satu grup berlabel "Rentang tahun"
        JPanel yearRange = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        yearRange.setOpaque(false);
        yearRange.add(fromYearField);
        yearRange.add(createInlineLabel("hingga"));
        yearRange.add(toYearField);
        panel.add(createLabeledSearchField("Rentang tahun", yearRange));

        Font searchFont = keywordField.getFont();
        for (Component component : new Component[]{advancedAffiliationField, sintaComboBox, fromYearField, toYearField}) {
            component.setFont(searchFont);
        }
        return panel;
    }

    private JLabel createInlineLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(new Color(75, 75, 75));
        return label;
    }

    private void layoutContentLayer() {
        int headerHeight = headerToolbar.getPreferredSize().height;
        boolean filterOpen = advancedSearchPanel.isVisible();
        int filterHeight = filterOpen ? advancedSearchPanel.getPreferredSize().height : 0;

        headerToolbar.setBounds(0, 0, contentLayer.getWidth(), headerHeight);

        // Sidebar tidak ikut turun: tetap menempel di bawah bar pencarian
        sidebar.setBounds(0, headerHeight, SIDEBAR_WIDTH,
                Math.max(0, contentLayer.getHeight() - headerHeight));

        // Baris filter lanjutan hanya mengisi area di kanan sidebar (rata dengan bar)
        if (filterOpen) {
            advancedSearchPanel.setBounds(SIDEBAR_WIDTH + 5, headerHeight,
                    Math.max(0, contentLayer.getWidth() - SIDEBAR_WIDTH - 5),
                    advancedSearchPanel.getPreferredSize().height);
        }

        // Hanya konten tengah (tabel + inspector) yang turun mengikuti baris filter
        int contentTop = headerHeight + filterHeight;
        mainSplit.setBounds(SIDEBAR_WIDTH + 5, contentTop,
                Math.max(0, contentLayer.getWidth() - SIDEBAR_WIDTH - 5),
                Math.max(0, contentLayer.getHeight() - contentTop));
        updateSearchControlsWidth();
    }

    /** Menghitung lebar bar pencarian: dari tepi kiri wrapper hingga margin kanan header (mentok kanan). */
    private void updateSearchControlsWidth() {
        if (searchControls == null || searchControls.getParent() == null || contentLayer == null) return;
        Container searchArea = searchControls.getParent();
        Point areaLeft = SwingUtilities.convertPoint(searchArea, 0, 0, contentLayer);
        int width = Math.max(460, contentLayer.getWidth() - areaLeft.x - 14);
        Dimension pref = searchControls.getPreferredSize();
        if (width != pref.width) {
            searchControls.setPreferredSize(new Dimension(width, pref.height));
            searchControls.revalidate();
        }
    }

    /** Inspector hanya muncul saat ada artikel yang dipilih di tabel. */
    private void showArticleInInspector(Article article) {
        inspectorPanel.setArticle(article);
        boolean show = article != null;
        inspectorPanel.setVisible(show);
        mainSplit.setDividerSize(show ? 6 : 0);
        if (show) {
            // JSplitPane memberi seluruh ruang ke tabel saat inspector tidak visible;
            // saat muncul kembali, kembalikan proporsi 68 : 32
            mainSplit.setDividerLocation(0.68);
        }
    }

    /** Toggle baris filter lanjutan: memunculkan/menyembunyikan baris kedua di header. */
    private void toggleAdvancedSearch(JButton anchor) {
        boolean visible = !advancedSearchPanel.isVisible();
        advancedSearchPanel.setVisible(visible);
        anchor.setText(visible ? "Pencarian Lanjutan ▴" : "Pencarian Lanjutan ▾");
        // Tinggi header berubah saat panel muncul/hilang → hitung ulang batas konten
        layoutContentLayer();
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
            crawlBtn.setText("🔍 Cari");
            crawlBtn.setBackground(SEARCH_BTN_BLUE);
            crawlBtn.setForeground(Color.WHITE);
            tablePanel.setProgress(false, "Penelusuran dihentikan oleh pengguna.");
            tablePanel.setPaginationEnabled(true);
            return;
        }

        String keyword = buildAdvancedQuery();
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
        this.sintaPoolExhausted = false;
        this.sintaFilteredPool.clear();

        // Reset cache dan tabel untuk pencarian baru
        garudaRawPageCache.clear();
        pageCache.clear();
        masterArticleList.clear();
        tablePanel.clearArticles();
        showArticleInInspector(null); // sembunyikan inspector saat pencarian baru

        loadPage(1);
    }

    private String buildAdvancedQuery() {
        List<String> terms = new ArrayList<>();
        if (!keywordField.getText().isBlank()) terms.add(keywordField.getText().strip());
        if (!advancedJournalField.getText().isBlank()) terms.add(advancedJournalField.getText().strip());
        if (!advancedAuthorField.getText().isBlank()) terms.add(advancedAuthorField.getText().strip());
        if (!advancedAffiliationField.getText().isBlank()) terms.add(advancedAffiliationField.getText().strip());
        return String.join(" ", terms).strip();
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
            // Status dibiarkan kosong saat idle agar tidak menyingkirkan kontrol paginasi
            tablePanel.setProgress(false, "");
            return;
        }

        crawlBtn.setText("■ Stop");
        crawlBtn.setBackground(SEARCH_BTN_RED);
        crawlBtn.setForeground(Color.WHITE);
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

                    while (!sintaPoolExhausted && sintaFilteredPool.size() < targetQuota) {
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

                        if (!res.hasMore() || res.articles().isEmpty()) {
                            // Hasil sesuai filter akreditasi sudah habis (walau halaman
                            // mentah Garuda masih banyak) — hentikan pemindaian agar
                            // paginasi tidak terkunci memindai ratusan halaman kosong
                            sintaPoolExhausted = true;
                            break;
                        }
                    }

                    if (sintaPoolExhausted || sintaLastGarudaPageScanned >= totalGarudaPages) {
                        totalRecordsFound = sintaFilteredPool.size();
                        totalSearchPages = Math.max(page,
                                Math.max(1, (int) Math.ceil((double) totalRecordsFound / activePageSize)));
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
                    // Informasi halaman/total sudah ada di label kiri & indikator "1 / N";
                    // status dibiarkan kosong agar kontrol paginasi tidak tertimpa teks panjang
                    tablePanel.setProgress(false, "");

                    saveSearchOutput();
                } catch (Exception e) {
                    tablePanel.setProgress(false, "Gagal memuat Halaman " + page + ": " + e.getMessage());
                }
            }
        };

        currentSearchWorker.execute();
    }

    /**
     * Menu "File → Simpan Hasil Pencarian": menyimpan akumulasi hasil crawl saat ini
     * (masterArticleList, sudah dideduplikasi) ke berkas JSON pilihan pengguna.
     * Dialog dibuka di folder proyek aktif.
     */
    private void saveResultsManually() {
        if (masterArticleList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Belum ada hasil pencarian untuk disimpan. Jalankan pencarian terlebih dahulu.",
                    "Simpan Hasil Pencarian",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(activeProject.toFile());
        chooser.setDialogTitle("Simpan Hasil Pencarian");
        chooser.setSelectedFile(new File("articles.json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith(".json")) {
            target = target.resolveSibling(target.getFileName().toString() + ".json");
        }
        if (Files.exists(target)) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Berkas \"" + target.getFileName() + "\" sudah ada. Timpa?",
                    "Simpan Hasil Pencarian", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        try {
            ArticleStorage.saveJson(target, masterArticleList);
            JOptionPane.showMessageDialog(this,
                    "Berhasil menyimpan " + masterArticleList.size() + " artikel ke:\n" + target,
                    "Simpan Hasil Pencarian", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal menyimpan: " + ex.getMessage(),
                    "Simpan Hasil Pencarian", JOptionPane.ERROR_MESSAGE);
        }
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
