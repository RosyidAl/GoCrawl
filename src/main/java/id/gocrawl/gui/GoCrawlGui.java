package id.gocrawl.gui;

import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Jendela Utama GoCrawl Desktop GUI:
 * Mengusung tata letak 2-panel bersih (Tabel Hasil Penelusuran & Detail Inspector Metadata),
 * terintegrasi langsung dengan mesin pencarian Portal Garuda, kontrol paginasi per-halaman,
 * dialog direktori jurnal terindeks, penyaring akreditasi SINTA, dan navigator koleksi artikel.
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

    /** Warna bilah judul abu-abu dan hover menu-nya. */
    private static final Color TITLE_BAR_BACKGROUND = new Color(80, 86, 94);
    private static final Color TITLE_BAR_HOVER = new Color(97, 103, 112);

    /** Lebar kolom sidebar; sekaligus garis potong kiri bar pencarian (tepi kanan sidebar). */
    private static final int SIDEBAR_WIDTH = 220;

    private final JTextField keywordField = new JTextField(20);
    private final JComboBox<SintaFilterItem> sintaComboBox = new JComboBox<>();
    private final JTextField fromYearField = new JTextField("2021", 5);
    private final JTextField toYearField = new JTextField("2026", 5);
    private final JButton crawlBtn = new JButton("Cari");
    private final JTextField advancedJournalField = new JTextField();
    private final JTextField advancedAuthorField = new JTextField();
    private final JTextField advancedAffiliationField = new JTextField();
    private JPanel advancedSearchPanel;
    private JButton advancedSearchButton;
    private JPanel searchHeaderContent;
    private JPanel headerToolbar;
    private JSplitPane mainSplit;
    private JPanel sidebar;
    private JPanel brandBox;
    private JButton sidebarToggleBtn;
    private DefaultListModel<Path> collectionModel;
    private JList<Path> collectionList;
    private JLabel emptyCollectionsLabel;
    private boolean koleksiListExpanded = true;
    private boolean updatingCollectionList;
    private final WorkspaceController workspace = new WorkspaceController();

    /** View yang sedang ditampilkan di tabel: hasil crawl atau satu koleksi. */
    private enum SidebarView { SEARCH_RESULTS, COLLECTION }
    private SidebarView activeView = SidebarView.SEARCH_RESULTS;
    private JButton navSearchRow;
    private JButton navFavoritesRow;

    private JournalCatalog catalog;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;
    private List<CrawlSource> crawlSources;
    private SearchController searchController;

    public GoCrawlGui() {
        super("GoCrawl Desktop â€” Academic Research Engine");
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
        this.crawlSources = CrawlSourceStore.load(Path.of("data", "crawl_sources.txt"));
        this.searchController = new SearchController(
                catalog,
                crawlSources,
                Path.of("data", "crawl_sources.txt"),
                new SearchController.Listener() {
                    @Override
                    public void onStarted(String status) {
                        setSearchRunning(true, status);
                    }

                    @Override
                    public void onProgress(String status) {
                        tablePanel.setProgress(true, status);
                    }

                    @Override
                    public void onFinished() {
                        setSearchRunning(false, null);
                    }

                    @Override
                    public void onPageLoaded(SearchController.Page page) {
                        tablePanel.setArticles(page.articles());
                        tablePanel.updatePagination(page.page(), page.totalPages(),
                                page.totalRecords(), page.articles().size());
                        tablePanel.setProgress(false, "");
                        try {
                            workspace.saveSearchOutput(searchController.accumulatedArticles());
                        } catch (Exception ignored) {
                            // Auto-save bersifat best effort; hasil tetap tampil di tabel.
                        }
                    }

                    @Override
                    public void onCancelled() {
                        tablePanel.setProgress(false, "Pemuatan halaman dihentikan.");
                    }

                    @Override
                    public void onFailure(String message) {
                        tablePanel.setProgress(false, message);
                    }

                    @Override
                    public void onSourceUnavailable() {
                        showWarning("Sumber Crawl",
                                "Tidak ada sumber crawl yang aktif. Atur di Katalog > Kelola Sumber Crawl...");
                    }

                    @Override
                    public void onSourceFallback(String message) {
                        tablePanel.setProgress(false, message);
                    }
                });
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 820);
        setMinimumSize(new Dimension(980, 580));
        setLocationRelativeTo(null);

        // 1. Menu Bar Desktop
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem saveResultsItem = new JMenuItem("Simpan Hasil Pencarian...");
        saveResultsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveResultsItem.addActionListener(e -> saveResultsManually());

        JMenuItem openProjectFolderItem = new JMenuItem("Buka Folder Koleksi...");
        openProjectFolderItem.addActionListener(e -> chooseProjectFolder());

        JMenuItem exitItem = new JMenuItem("Keluar");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveResultsItem);
        fileMenu.add(openProjectFolderItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu catalogMenu = new JMenu("Katalog");
        JMenuItem manageSourcesItem = new JMenuItem("Kelola Sumber Crawl...");
        manageSourcesItem.addActionListener(e -> new CrawlSourcesDialog(this, crawlSources,
                Path.of("data", "crawl_sources.txt"), null).setVisible(true));
        catalogMenu.add(manageSourcesItem);
        menuBar.add(catalogMenu);

        setJMenuBar(menuBar);
        // Bilah judul abu-abu: client property ini menonaktifkan bilah "unified"
        // FlatLaf (yang mengikuti latar window putih) dan memberi warna judul
        getRootPane().putClientProperty("JRootPane.titleBarBackground", TITLE_BAR_BACKGROUND);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);
        menuBar.setForeground(Color.WHITE);
        for (MenuElement item : menuBar.getSubElements()) {
            if (item instanceof JMenuItem) ((JMenuItem) item).setForeground(Color.WHITE);
        }

        // 2. Toolbar atas dengan panel filter lanjutan yang ikut mengalir secara natural.
        headerToolbar = new JPanel(new BorderLayout(10, 0));
        headerToolbar.setBorder(new EmptyBorder(10, 14, 10, 14));

        brandBox = new JPanel(new BorderLayout());
        brandBox.setOpaque(false);
        brandBox.setPreferredSize(new Dimension(0, 1));
        sidebarToggleBtn = new JButton("≡");
        flatIconButton(sidebarToggleBtn, "Tampilkan / sembunyikan sidebar");
        sidebarToggleBtn.setForeground(Color.WHITE);
        sidebarToggleBtn.addActionListener(e -> toggleSidebar());
        brandBox.add(sidebarToggleBtn, BorderLayout.WEST);

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
        crawlBtn.setText("Cari");
        crawlBtn.setFont(crawlBtn.getFont().deriveFont(Font.BOLD, 12f));
        crawlBtn.setBackground(SEARCH_BTN_BLUE);
        crawlBtn.setForeground(Color.WHITE);
        crawlBtn.setPreferredSize(new Dimension(95, 30));

        advancedSearchPanel = createAdvancedSearchPanel();
        advancedSearchPanel.setVisible(false);
        advancedSearchButton = new JButton("Pencarian Lanjutan â–¾");
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

        JPanel searchControls = createSearchBar(advancedSearchButton);
        searchHeaderContent = new JPanel(new BorderLayout(0, 4));
        searchHeaderContent.setOpaque(false);
        JPanel searchArea = new JPanel(new BorderLayout());
        searchArea.setOpaque(false);
        searchArea.add(searchControls, BorderLayout.CENTER);
        searchHeaderContent.add(searchArea, BorderLayout.NORTH);
        searchHeaderContent.add(advancedSearchPanel, BorderLayout.SOUTH);
        headerToolbar.add(searchHeaderContent, BorderLayout.CENTER);


        // 3. Tata Letak Bersih 2-Panel (Tabel Artikel di Kiri/Tengah & Detail Inspector di Kanan)
        inspectorPanel = new DetailInspectorPanel(catalog);
        // Inspector disembunyikan di awal; muncul hanya saat artikel dipilih di tabel
        inspectorPanel.setVisible(false);
        tablePanel = new ArticleTablePanel(catalog, this::showArticleInInspector);
        tablePanel.setOnAddToFavorites(this::addArticleToFavorites);
        tablePanel.setOnAddToCollectionRequest(this::showAddToCollectionDialog);
        tablePanel.setOnRemoveFromCollection(this::removeFromActiveCollection);
        tablePanel.setOnPageChange(searchController::loadPage);
        tablePanel.setOnPageSizeChange(searchController::setPageSize);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        mainSplit.setResizeWeight(0.68);
        mainSplit.setDividerSize(6);

        sidebar = createSidebar();

        JPanel content = new JPanel(new BorderLayout());
        JPanel resultsArea = new JPanel(new BorderLayout());
        resultsArea.add(headerToolbar, BorderLayout.NORTH);
        resultsArea.add(mainSplit, BorderLayout.CENTER);
        content.add(sidebar, BorderLayout.WEST);
        content.add(resultsArea, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // Pesan Awal
        tablePanel.setProgress(false, "Ketikkan kata kunci pencarian lalu tekan Enter atau klik 'Mulai Crawl'.");

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        keywordField.addActionListener(e -> handleCrawlToggle());
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBorder(new EmptyBorder(16, 14, 14, 12));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 500));
        sidebar.setBackground(new Color(248, 249, 252));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        navSearchRow = createNavRow("Hasil Pencarian", this::showSearchResults);
        navFavoritesRow = createNavRow("Favorit", this::openFavorites);
        nav.add(navSearchRow);
        nav.add(navFavoritesRow);
        sidebar.add(nav, BorderLayout.NORTH);

        JPanel koleksiSection = new JPanel(new BorderLayout(0, 2));
        koleksiSection.setOpaque(false);

        JPanel koleksiHeader = new JPanel(new BorderLayout(2, 0));
        koleksiHeader.setOpaque(false);
        BasicArrowButton expandBtn = new BasicArrowButton(
                koleksiListExpanded ? SwingConstants.SOUTH : SwingConstants.EAST);
        flatIconButton(expandBtn, "Lipat / bentangkan daftar koleksi");
        expandBtn.addActionListener(e -> {
            koleksiListExpanded = !koleksiListExpanded;
            expandBtn.setDirection(koleksiListExpanded ? SwingConstants.SOUTH : SwingConstants.EAST);
            refreshKoleksiList();
        });
        JLabel koleksiTitle = new JLabel("Koleksi");
        koleksiTitle.setFont(koleksiTitle.getFont().deriveFont(Font.BOLD, 13f));
        koleksiTitle.setForeground(new Color(60, 60, 60));
        JButton addBtn = new JButton("+");
        flatIconButton(addBtn, "Buat koleksi baru");
        addBtn.addActionListener(e -> createProjectFolder());
        koleksiHeader.add(expandBtn, BorderLayout.WEST);
        koleksiHeader.add(koleksiTitle, BorderLayout.CENTER);
        koleksiHeader.add(addBtn, BorderLayout.EAST);
        koleksiSection.add(koleksiHeader, BorderLayout.NORTH);

        collectionModel = new DefaultListModel<>();
        collectionList = new JList<>(collectionModel);
        collectionList.setOpaque(false);
        collectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        collectionList.setFixedCellHeight(30);
        collectionList.setBorder(new EmptyBorder(2, 12, 2, 0));
        collectionList.setSelectionBackground(new Color(228, 235, 251));
        collectionList.setSelectionForeground(new Color(48, 78, 158));
        collectionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focused);
                Path path = (Path) value;
                label.setText(workspace.displayName(path) + "  (" + workspace.articleCount(path) + ")");
                label.setToolTipText(path.toString());
                label.setBorder(new EmptyBorder(6, 12, 6, 6));
                if (!selected) {
                    label.setBackground(new Color(248, 249, 252));
                    label.setForeground(new Color(60, 60, 60));
                }
                return label;
            }
        });
        collectionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updatingCollectionList && collectionList.getSelectedValue() != null) {
                openCollection(collectionList.getSelectedValue());
            }
        });
        collectionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showCollectionContextMenu(e); }

            @Override
            public void mouseReleased(MouseEvent e) { showCollectionContextMenu(e); }
        });

        JScrollPane koleksiScroll = new JScrollPane(collectionList);
        koleksiScroll.setBorder(null);
        koleksiScroll.setOpaque(false);
        koleksiScroll.getViewport().setOpaque(false);
        koleksiScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        koleksiScroll.getVerticalScrollBar().setUnitIncrement(16);

        emptyCollectionsLabel = new JLabel("Belum ada koleksi");
        emptyCollectionsLabel.setFont(emptyCollectionsLabel.getFont().deriveFont(Font.ITALIC, 11f));
        emptyCollectionsLabel.setForeground(new Color(150, 150, 150));
        emptyCollectionsLabel.setBorder(new EmptyBorder(4, 12, 4, 4));
        JPanel collectionListArea = new JPanel(new BorderLayout());
        collectionListArea.setOpaque(false);
        collectionListArea.add(emptyCollectionsLabel, BorderLayout.NORTH);
        collectionListArea.add(koleksiScroll, BorderLayout.CENTER);
        koleksiSection.add(collectionListArea, BorderLayout.CENTER);

        sidebar.add(koleksiSection, BorderLayout.CENTER);
        refreshKoleksiList();
        refreshNavHighlight();
        return sidebar;
    }

    /** Tombol ikon polos tanpa border (gaya toolBarButton), tanpa cincin fokus. */
    private void flatIconButton(JButton button, String tooltip) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setMargin(new Insets(2, 6, 2, 6));
        button.setFocusable(false); // cegah border biru fokus setelah diklik
    }

    /** Menampilkan/menyembunyikan sidebar kiri. */
    private void toggleSidebar() {
        boolean visible = !sidebar.isVisible();
        sidebar.setVisible(visible);
        int hiddenWidth = sidebarToggleBtn.getPreferredSize().width + 4;
        brandBox.setPreferredSize(new Dimension(
                visible ? SIDEBAR_WIDTH : hiddenWidth,
                brandBox.getPreferredSize().height));
        headerToolbar.revalidate();
        revalidate();
        repaint();
    }

    /** Baris navigasi (Hasil Pencarian / Favorit) yang bisa diklik untuk berganti view. */
    private JButton createNavRow(String label, Runnable action) {
        JButton row = new JButton(label);
        row.setOpaque(true);
        row.setBorder(new EmptyBorder(10, 9, 10, 6));
        row.setHorizontalAlignment(SwingConstants.LEFT);
        row.setContentAreaFilled(true);
        row.setFocusable(false);
        row.addActionListener(e -> action.run());
        return row;
    }

    /** Mewarnai baris nav sesuai view yang sedang aktif. */
    private void refreshNavHighlight() {
        if (navSearchRow == null || navFavoritesRow == null) return;
        applyRowStyle(navSearchRow, activeView == SidebarView.SEARCH_RESULTS);
        applyRowStyle(navFavoritesRow, activeView == SidebarView.COLLECTION
                && workspace.activeProject().equals(workspace.favoritesPath()));
    }

    private void applyRowStyle(JButton row, boolean active) {
        row.setFont(row.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 12f));
        row.setBackground(active ? new Color(228, 235, 251) : new Color(248, 249, 252));
        row.setForeground(active ? new Color(48, 78, 158) : new Color(60, 60, 60));
    }

    /** Menyusun ulang daftar koleksi (folder di projects/) dengan koleksi aktif disorot. */
    private void refreshKoleksiList() {
        if (collectionList == null) return;
        updatingCollectionList = true;
        try {
            collectionModel.clear();
            List<Path> collections = workspace.sidebarCollections();
            Path defaultWorkspace = workspace.defaultWorkspace();
            boolean empty = collections.isEmpty()
                    || (collections.size() == 1 && collections.contains(defaultWorkspace));
            emptyCollectionsLabel.setVisible(koleksiListExpanded && empty);
            collectionList.setVisible(koleksiListExpanded && !empty);
            if (koleksiListExpanded && !empty) {
                collections.forEach(collectionModel::addElement);
                if (activeView == SidebarView.COLLECTION) {
                    collectionList.setSelectedValue(workspace.activeProject(), true);
                } else {
                    collectionList.clearSelection();
                }
            } else {
                collectionList.clearSelection();
            }
        } finally {
            updatingCollectionList = false;
        }
        collectionList.revalidate();
        collectionList.repaint();
    }

    private boolean isCollectionInView(Path path) {
        return activeView == SidebarView.COLLECTION
                && workspace.activeProject().equals(path.toAbsolutePath().normalize());
    }

    private void showCollectionContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int index = collectionList.locationToIndex(e.getPoint());
        if (index < 0 || !collectionList.getCellBounds(index, index).contains(e.getPoint())) return;
        Path path = collectionModel.getElementAt(index);
        if (!workspace.canDelete(path)) return;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Hapus Koleksi...");
        deleteItem.addActionListener(ev -> deleteCollection(path));
        menu.add(deleteItem);
        menu.show(collectionList, e.getX(), e.getY());
    }

    /** Menghapus folder koleksi secara rekursif setelah konfirmasi pengguna. */
    private void deleteCollection(Path path) {
        if (!workspace.canDelete(path)) return;
        if (!confirm("Hapus Koleksi",
                "Hapus koleksi \"" + workspace.displayName(path) + "\" beserta seluruh isinya?",
                JOptionPane.WARNING_MESSAGE)) return;
        boolean wasActive = isCollectionInView(path);
        try {
            workspace.deleteCollection(path);
        } catch (Exception ex) {
            showError("Hapus Koleksi", "Gagal menghapus koleksi: " + ex.getMessage());
            return;
        }
        if (wasActive) {
            showSearchResults();
        } else {
            refreshKoleksiList();
        }
    }

    private void setActiveProject(Path path) {
        workspace.setActiveProject(path);
        refreshKoleksiList();
    }

    private void chooseProjectFolder() {
        JFileChooser chooser = new JFileChooser(workspace.activeProject().toFile());
        chooser.setDialogTitle("Pilih folder koleksi");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setActiveProject(chooser.getSelectedFile().toPath().toAbsolutePath().normalize());
        }
    }

    private void createProjectFolder() {
        String name = JOptionPane.showInputDialog(this, "Nama koleksi baru:", "Buat Koleksi", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        try {
            setActiveProject(workspace.createCollection(name));
        } catch (IllegalArgumentException ex) {
            showWarning("Buat Koleksi", ex.getMessage());
        } catch (Exception ex) {
            showError("Buat Koleksi", "Gagal membuat koleksi: " + ex.getMessage());
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
        anchor.setText(visible ? "Pencarian Lanjutan ^" : "Pencarian Lanjutan v");
        searchHeaderContent.revalidate();
        searchHeaderContent.repaint();
    }

    private String buildAdvancedQuery() {
        List<String> terms = new ArrayList<>();
        if (!keywordField.getText().isBlank()) terms.add(keywordField.getText().strip());
        if (!advancedJournalField.getText().isBlank()) terms.add(advancedJournalField.getText().strip());
        if (!advancedAuthorField.getText().isBlank()) terms.add(advancedAuthorField.getText().strip());
        if (!advancedAffiliationField.getText().isBlank()) terms.add(advancedAffiliationField.getText().strip());
        return String.join(" ", terms).strip();
    }

    private void handleCrawlToggle() {
        if (searchController.isRunning()) {
            searchController.cancel();
            setSearchRunning(false, null);
            tablePanel.setProgress(false, "Penelusuran dihentikan oleh pengguna.");
            return;
        }

        String keyword = buildAdvancedQuery();
        if (keyword.isEmpty()) {
            showWarning("Validasi Kata Kunci",
                    "Ketikkan kata kunci pencarian terlebih dahulu (cth: 'pendidikan karakter', 'deep learning').");
            return;
        }

        Integer fromYear = null;
        Integer toYear = null;
        try {
            if (!fromYearField.getText().isBlank()) fromYear = Integer.parseInt(fromYearField.getText().strip());
            if (!toYearField.getText().isBlank()) toYear = Integer.parseInt(toYearField.getText().strip());
            if (fromYear != null && toYear != null && fromYear > toYear) {
                showWarning("Validasi Tahun", "Tahun awal tidak boleh melebihi tahun akhir.");
                return;
            }
        } catch (NumberFormatException ex) {
            showWarning("Validasi Tahun", "Format tahun harus berupa angka.");
            return;
        }

        SintaFilterItem selectedSinta = (SintaFilterItem) sintaComboBox.getSelectedItem();
        activeView = SidebarView.SEARCH_RESULTS;
        searchHeaderContent.setVisible(true);
        searchHeaderContent.revalidate();
        refreshNavHighlight();
        refreshKoleksiList();
        tablePanel.setBrowseMode(false);
        tablePanel.clearArticles();
        showArticleInInspector(null);

        searchController.start(new SearchController.Request(
                keyword,
                selectedSinta != null ? selectedSinta.id() : "ALL",
                fromYear,
                toYear,
                tablePanel.getPageSize()));
    }

    private void setSearchRunning(boolean running, String status) {
        crawlBtn.setText(running ? "Stop" : "Cari");
        crawlBtn.setBackground(running ? SEARCH_BTN_RED : SEARCH_BTN_BLUE);
        crawlBtn.setForeground(Color.WHITE);
        tablePanel.setPaginationEnabled(!running);
        if (status != null) tablePanel.setProgress(running, status);
    }

    /** Menyimpan akumulasi hasil pencarian ke berkas JSON pilihan pengguna. */
    private void saveResultsManually() {
        List<Article> articles = searchController.accumulatedArticles();
        if (articles.isEmpty()) {
            showWarning("Simpan Hasil Pencarian",
                    "Belum ada hasil pencarian untuk disimpan. Jalankan pencarian terlebih dahulu.");
            return;
        }

        JFileChooser chooser = new JFileChooser(workspace.activeProject().toFile());
        chooser.setDialogTitle("Simpan Hasil Pencarian");
        chooser.setSelectedFile(new File("articles.json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith(".json")) {
            target = target.resolveSibling(target.getFileName().toString() + ".json");
        }
        if (java.nio.file.Files.exists(target)) {
            if (!confirm("Simpan Hasil Pencarian",
                    "Berkas \"" + target.getFileName() + "\" sudah ada. Timpa?")) return;
        }
        try {
            workspace.saveJson(target, articles);
            showInfo("Simpan Hasil Pencarian",
                    "Berhasil menyimpan " + articles.size() + " artikel ke:\n" + target);
        } catch (Exception ex) {
            showError("Simpan Hasil Pencarian", "Gagal menyimpan: " + ex.getMessage());
        }
    }

    private void cancelRunningSearch() {
        searchController.cancel();
        setSearchRunning(false, null);
    }

    /** Menampilkan kembali hasil pencarian terakhir. */
    private void showSearchResults() {
        cancelRunningSearch();
        activeView = SidebarView.SEARCH_RESULTS;
        refreshNavHighlight();
        refreshKoleksiList();
        searchHeaderContent.setVisible(true);
        searchHeaderContent.revalidate();
        tablePanel.setBrowseMode(false);

        SearchController.Page page = searchController.visiblePage();
        tablePanel.setArticles(page.articles());
        tablePanel.updatePagination(page.page(), page.totalPages(), page.totalRecords(), page.articles().size());
        tablePanel.setProgress(false, page.totalRecords() == 0
                ? "Belum ada pencarian — jalankan crawl untuk melihat hasil."
                : "");
    }

    private Path favoritesPath() {
        return workspace.favoritesPath();
    }

    private void openFavorites() {
        try {
            workspace.ensureFavorites();
        } catch (Exception ignored) {
        }
        openCollection(favoritesPath());
    }

    private void addArticleToFavorites(Article article) {
        boolean added = appendArticleToCollection(favoritesPath(), article);
        if (added) {
            tablePanel.setProgress(false, "★ Ditambahkan ke Favorit");
            if (activeView == SidebarView.COLLECTION
                    && workspace.activeProject().equals(favoritesPath())) {
                openCollection(favoritesPath());
            }
        } else {
            tablePanel.setProgress(false, "Artikel sudah ada di Favorit");
        }
    }

    private boolean appendArticleToCollection(Path collection, Article article) {
        try {
            return workspace.addToCollection(collection, article);
        } catch (Exception ex) {
            showError("Tambah ke Koleksi", "Gagal menambahkan ke koleksi: " + ex.getMessage());
            return false;
        }
    }

    private void showAddToCollectionDialog(Article article) {
        List<Path> collections = workspace.userCollections();
        if (collections.isEmpty()) {
            showInfo("Tambah ke Koleksi",
                    "Belum ada koleksi. Buat dulu lewat tombol + di sidebar.");
            return;
        }

        JComboBox<Path> combo = new JComboBox<>(collections.toArray(new Path[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                Component component = super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(workspace.displayName((Path) value));
                return component;
            }
        });
        String title = article.title();
        if (title.length() > 48) title = title.substring(0, 45) + "...";
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Pilih tujuan koleksi untuk \"" + title + "\":"), BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        int choice = JOptionPane.showConfirmDialog(this, panel, "Tambahkan ke Koleksi",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        Path target = (Path) combo.getSelectedItem();
        if (appendArticleToCollection(target, article)) {
            showInfo("Tambah ke Koleksi",
                    "Artikel ditambahkan ke koleksi \"" + workspace.displayName(target) + "\".");
        } else {
            showInfo("Tambah ke Koleksi", "Artikel sudah ada di koleksi tersebut.");
        }
    }

    private void openCollection(Path path) {
        cancelRunningSearch();
        activeView = SidebarView.COLLECTION;
        setActiveProject(path);
        refreshNavHighlight();
        searchHeaderContent.setVisible(false);
        if (advancedSearchPanel.isVisible()) toggleAdvancedSearch(advancedSearchButton);

        List<Article> saved = workspace.loadCollection(path);
        tablePanel.setArticles(saved);
        tablePanel.updatePagination(1, 1, saved.size(), saved.size());
        tablePanel.setProgress(false, saved.isEmpty()
                ? "Koleksi \"" + workspace.displayName(path) + "\" masih kosong."
                : "");
        tablePanel.setBrowseMode(true);
    }

    private void removeFromActiveCollection(Article article) {
        if (activeView != SidebarView.COLLECTION) return;
        Path collection = workspace.activeProject();
        if (!confirm("Buang dari Koleksi",
                "Buang artikel ini dari koleksi \"" + workspace.displayName(collection) + "\"?")) return;
        try {
            List<Article> existing = workspace.removeFromCollection(collection, article);
            tablePanel.setArticles(existing);
            tablePanel.updatePagination(1, 1, existing.size(), existing.size());
            refreshKoleksiList();
        } catch (Exception ex) {
            showError("Buang dari Koleksi", "Gagal membuang artikel: " + ex.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirm(String title, String message) {
        return confirm(title, message, JOptionPane.QUESTION_MESSAGE);
    }

    private boolean confirm(String title, String message, int messageType) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, messageType) == JOptionPane.YES_OPTION;
    }

    private static void applyTitleBarTheme() {
        // TitlePane.background dipakai FlatTitlePane sebagai warna bilah saat aktif,
        // TAPI hanya efektif bila client property JRootPane.titleBarBackground juga
        // diset di initUI â€” tanpa itu bilah "unified" mengikuti latar window putih
        UIManager.put("TitlePane.background", TITLE_BAR_BACKGROUND);
        UIManager.put("TitlePane.embeddedForeground", Color.WHITE);
        // Menu File/Katalog yang menempel di bilah judul ikut senada
        UIManager.put("MenuBar.background", TITLE_BAR_BACKGROUND);
        UIManager.put("MenuBar.foreground", Color.WHITE);
        UIManager.put("MenuBar.itemHoverBackground", TITLE_BAR_HOVER);
        // Tombol tutup memakai aksen merah khas Windows saat di-hover
        UIManager.put("TitlePane.closeHoverBackground", new Color(232, 17, 35));
        UIManager.put("TitlePane.closePressedBackground", new Color(249, 56, 66));
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                applyTitleBarTheme();
            } catch (Exception ignored) {}
            new GoCrawlGui().setVisible(true);
        });
    }
}
