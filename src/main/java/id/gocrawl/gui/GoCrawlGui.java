package id.gocrawl.gui;

import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import id.gocrawl.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private static final Color SEARCH_BTN_BLUE = AcademicTheme.PRIMARY;
    private static final Color SEARCH_BTN_RED = AcademicTheme.DANGER;

    /** Warna bilah judul terang senada sidebar. */
    private static final Color TITLE_BAR_BACKGROUND = AcademicTheme.SURFACE_LOW;

    /** Lebar kolom sidebar; sekaligus garis potong kiri bar pencarian (tepi kanan sidebar). */
    private static final int SIDEBAR_WIDTH = 236;

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
    private JLayeredPane contentLayer;
    private JPanel searchControls;
    private JPanel headerToolbar;
    private JSplitPane mainSplit;
    private JPanel sidebar;
    private JPanel brandBox;
    private JButton sidebarToggleBtn;
    private JPanel koleksiListPanel;
    private boolean koleksiListExpanded = true;
    private final WorkspaceController workspace = new WorkspaceController();

    /** View yang sedang ditampilkan di tabel: hasil crawl atau satu koleksi. */
    private enum SidebarView { SEARCH_RESULTS, COLLECTION }
    private SidebarView activeView = SidebarView.SEARCH_RESULTS;
    private RoundedPanel navSearchRow;
    private RoundedPanel navFavoritesRow;
    private JLabel navSearchLabel;
    private JLabel navFavoritesLabel;
    private PillLabel navFavoritesCount;

    private JournalCatalog catalog;
    private ArticleTablePanel tablePanel;
    private DetailInspectorPanel inspectorPanel;
    private List<CrawlSource> crawlSources;
    private SearchController searchController;
    private AWTEventListener focusReleaseMouseListener;

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
                        JOptionPane.showMessageDialog(GoCrawlGui.this,
                                "Tidak ada sumber crawl yang aktif. Atur di Katalog > Kelola Sumber Crawl...",
                                "Sumber Crawl", JOptionPane.WARNING_MESSAGE);
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

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("Tentang GoCrawl");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "GoCrawl Desktop — Academic Research Engine\n"
                        + "Penelusuran & kurasi literatur ilmiah Indonesia (Garuda, OJS, SINTA).\n"
                        + "Shortcut: Ctrl+K (Fokus Pencarian) · Ctrl+1 (Hasil Pencarian)",
                "Tentang GoCrawl", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
        // Bilah judul terang senada sidebar, bersih tanpa judul/ikon di tengah atas
        getRootPane().putClientProperty("JRootPane.titleBarBackground", TITLE_BAR_BACKGROUND);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", AcademicTheme.ON_SURFACE);
        getRootPane().putClientProperty("JRootPane.titleBarShowTitle", false);
        getRootPane().putClientProperty("JRootPane.titleBarShowIcon", false);
        menuBar.setForeground(AcademicTheme.ON_SURFACE_VARIANT);

        // 2. Toolbar Atas (Search Card 2 baris)
        headerToolbar = new JPanel(new BorderLayout(0, 0));
        headerToolbar.setBackground(AcademicTheme.SURFACE);
        headerToolbar.setBorder(new EmptyBorder(10, 14, 6, 14));

        // Kolom kiri header: spacer selebar sidebar agar bar pencarian mulai
        // persis di tepi kanan sidebar
        brandBox = new JPanel(new BorderLayout());
        brandBox.setOpaque(false);
        brandBox.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 14, 1));
        headerToolbar.add(brandBox, BorderLayout.WEST);

        // Inisialisasi ComboBox Akreditasi
        sintaComboBox.addItem(new SintaFilterItem("ALL", "Semua Akreditasi (Sinta 1-6 & Scopus)"));
        sintaComboBox.addItem(new SintaFilterItem("S1", "Minimal Sinta 1 (S1)"));
        sintaComboBox.addItem(new SintaFilterItem("S2", "Minimal Sinta 2 (S2)"));
        sintaComboBox.addItem(new SintaFilterItem("S3", "Minimal Sinta 3 (S3)"));
        sintaComboBox.addItem(new SintaFilterItem("S4", "Minimal Sinta 4 (S4)"));
        sintaComboBox.addItem(new SintaFilterItem("S5", "Minimal Sinta 5 (S5)"));
        sintaComboBox.addItem(new SintaFilterItem("S6", "Minimal Sinta 6 (S6)"));
        sintaComboBox.setPreferredSize(new Dimension(210, 32));
        sintaComboBox.setFont(AcademicTheme.uiFont(Font.PLAIN, 12.5f));
        sintaComboBox.setToolTipText("Filter batas minimal peringkat akreditasi jurnal");

        // Tombol Cari & Pencarian Lanjutan
        crawlBtn.setText("Cari");
        crawlBtn.setIcon(AcademicIcons.search(Color.WHITE, 14));
        AcademicTheme.stylePrimaryButton(crawlBtn);
        crawlBtn.setPreferredSize(new Dimension(96, 36));

        advancedSearchPanel = createAdvancedSearchPanel();
        advancedSearchPanel.setVisible(false);
        advancedSearchButton = new JButton("Pencarian Lanjutan", AcademicIcons.tune(AcademicTheme.ON_SURFACE_VARIANT, 14));
        AcademicTheme.styleSecondaryButton(advancedSearchButton);
        advancedSearchButton.setMargin(new Insets(0, 14, 0, 14));
        Dimension advPref = advancedSearchButton.getPreferredSize();
        advancedSearchButton.setPreferredSize(new Dimension(advPref.width + 6, 36));
        advancedSearchButton.setMinimumSize(new Dimension(advPref.width + 6, 36));
        advancedSearchButton.addActionListener(e -> toggleAdvancedSearch(advancedSearchButton));

        // Esc menutup baris filter lanjutan atau melepas fokus dari kolom input
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "handleEscape");
        getRootPane().getActionMap().put("handleEscape", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent evt) {
                if (advancedSearchPanel.isVisible()) {
                    toggleAdvancedSearch(advancedSearchButton);
                } else if (isTextComponentFocusedInWindow()) {
                    clearInputFocus();
                }
            }
        });

        // Shortcut global: Ctrl+K fokus pencarian, Ctrl+1 pindah ke Hasil Pencarian
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        getRootPane().getActionMap().put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent evt) {
                if (!headerToolbar.isVisible()) {
                    showSearchResults();
                }
                keywordField.requestFocusInWindow();
                keywordField.selectAll();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK), "goSearchResults");
        getRootPane().getActionMap().put("goSearchResults", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent evt) {
                showSearchResults();
            }
        });

        searchControls = createSearchBar(advancedSearchButton);
        headerToolbar.add(searchControls, BorderLayout.CENTER);


        // 3. Tata Letak Bersih 2-Panel (Tabel Artikel di Kiri/Tengah & Detail Inspector di Kanan)
        inspectorPanel = new DetailInspectorPanel(catalog);
        inspectorPanel.setOnClose(() -> {
            clearInputFocus();
            tablePanel.clearSelection();
            showArticleInInspector(null);
        });
        // Inspector disembunyikan di awal; muncul hanya saat artikel dipilih di tabel
        inspectorPanel.setVisible(false);
        tablePanel = new ArticleTablePanel(catalog, this::showArticleInInspector);
        tablePanel.setOnAddToFavorites(this::addArticleToFavorites);
        tablePanel.setOnAddToCollectionRequest(this::showAddToCollectionDialog);
        tablePanel.setOnRemoveFromCollection(this::removeFromActiveCollection);
        tablePanel.setOnPageChange(searchController::loadPage);
        tablePanel.setOnPageSizeChange(searchController::setPageSize);
        tablePanel.setOnSampleKeywordSelected(kw -> {
            keywordField.setText(kw);
            keywordField.requestFocusInWindow();
        });
        tablePanel.setOnFilterCleared(this::clearInputFocus);

        // Sidebar dan area konten diposisikan langsung di contentLayer (tanpa appSplit):
        // saat baris filter lanjutan terbuka, hanya area konten tengah yang turun â€”
        // sidebar tetap menempel di bawah bar pencarian
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inspectorPanel);
        mainSplit.setResizeWeight(0.68);
        mainSplit.setDividerSize(6);

        sidebar = createSidebar();

        contentLayer = new JLayeredPane();
        contentLayer.setLayout(null);
        contentLayer.add(headerToolbar, JLayeredPane.DEFAULT_LAYER);
        // Sidebar di layer lebih tinggi dari headerToolbar: keduanya overlap di tepi kiri
        contentLayer.add(sidebar, JLayeredPane.PALETTE_LAYER);
        contentLayer.add(mainSplit, JLayeredPane.DEFAULT_LAYER);
        contentLayer.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutContentLayer();
            }
        });
        add(contentLayer, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::layoutContentLayer);

        // Tombol toggle sidebar mengambang di bilah judul terang, tepat di sebelah
        // kiri tombol jendela (— □ ×). Posisinya dihitung di layoutContentLayer()
        sidebarToggleBtn = new JButton(AcademicIcons.panelLeft(AcademicTheme.ON_SURFACE_VARIANT, 15));
        flatIconButton(sidebarToggleBtn, "Tampilkan / sembunyikan sidebar");
        sidebarToggleBtn.setForeground(AcademicTheme.ON_SURFACE_VARIANT);
        sidebarToggleBtn.addActionListener(e -> toggleSidebar());
        getLayeredPane().add(sidebarToggleBtn, JLayeredPane.POPUP_LAYER);

        // Pesan Awal
        tablePanel.setProgress(false, "Ketikkan kata kunci pencarian lalu tekan Enter atau klik 'Cari'.");

        // Event Handlers
        crawlBtn.addActionListener(e -> handleCrawlToggle());
        keywordField.addActionListener(e -> handleCrawlToggle());

        installFocusReleaseSupport();
    }

    private void installFocusReleaseSupport() {
        contentLayer.setFocusable(true);
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            @Override
            public Component getInitialComponent(Window window) {
                return contentLayer;
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                clearInputFocus();
            }
        });

        focusReleaseMouseListener = event -> {
            if (!(event instanceof MouseEvent me) || me.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            Component source = me.getComponent();
            if (source == null || SwingUtilities.getWindowAncestor(source) != GoCrawlGui.this) {
                return;
            }
            if (isTextComponentFocusedInWindow() && shouldReleaseTextFocusOnClick(source, me)) {
                clearInputFocus();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(focusReleaseMouseListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private boolean isTextComponentFocusedInWindow() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner instanceof JTextComponent
                && SwingUtilities.getWindowAncestor(focusOwner) == this;
    }

    static boolean shouldReleaseTextFocusOnClick(Component source, MouseEvent event) {
        if (source == null) return false;
        if (source instanceof JTextComponent
                || SwingUtilities.getAncestorOfClass(JTextComponent.class, source) != null) {
            return false;
        }
        if (source instanceof JComboBox<?>
                || SwingUtilities.getAncestorOfClass(JComboBox.class, source) != null) {
            return false;
        }
        if (source instanceof JMenuBar || source instanceof JMenu || source instanceof JMenuItem
                || source instanceof JPopupMenu
                || SwingUtilities.getAncestorOfClass(JMenuBar.class, source) != null
                || SwingUtilities.getAncestorOfClass(JPopupMenu.class, source) != null) {
            return false;
        }
        if (source instanceof JScrollBar
                || SwingUtilities.getAncestorOfClass(JScrollBar.class, source) != null) {
            return false;
        }
        if (source instanceof JTable tbl && event != null && tbl.rowAtPoint(event.getPoint()) >= 0) {
            return false;
        }
        if (source instanceof AbstractButton btn && btn.isFocusable()) {
            return false;
        }
        return true;
    }

    void clearInputFocus() {
        if (contentLayer != null) {
            contentLayer.requestFocusInWindow();
        }
    }

    @Override
    public void dispose() {
        if (focusReleaseMouseListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(focusReleaseMouseListener);
            focusReleaseMouseListener = null;
        }
        super.dispose();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, AcademicTheme.BORDER_SUBTLE),
                new EmptyBorder(14, 10, 14, 10)
        ));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 500));
        sidebar.setBackground(AcademicTheme.SURFACE_LOW);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        navSearchLabel = new JLabel("Hasil Pencarian");
        navSearchRow = createNavRow(navSearchLabel, null, () -> showSearchResults());

        navFavoritesCount = new PillLabel("0");
        navFavoritesCount.setFont(AcademicTheme.uiFont(Font.BOLD, 10.5f));
        navFavoritesCount.setBorder(new EmptyBorder(2, 8, 2, 8));
        navFavoritesLabel = new JLabel("Favorit");
        navFavoritesRow = createNavRow(navFavoritesLabel, navFavoritesCount, () -> openFavorites());

        nav.add(navSearchRow);
        nav.add(Box.createVerticalStrut(4));
        nav.add(navFavoritesRow);
        sidebar.add(nav, BorderLayout.NORTH);

        // Seksi "KOLEKSI": header lipat + tombol tambah, daftar koleksi dengan ikon & hitungan
        JPanel koleksiSection = new JPanel(new BorderLayout(0, 4));
        koleksiSection.setOpaque(false);

        JPanel koleksiHeader = new JPanel(new BorderLayout(2, 0));
        koleksiHeader.setOpaque(false);
        koleksiHeader.setBorder(new EmptyBorder(4, 8, 2, 2));
        JLabel koleksiTitle = new JLabel("Koleksi",
                AcademicIcons.folder(AcademicTheme.ON_SURFACE_VARIANT, 15),
                SwingConstants.LEFT);
        koleksiTitle.setIconTextGap(6);
        koleksiTitle.setFont(AcademicTheme.uiFont(Font.BOLD, 13f));
        koleksiTitle.setForeground(AcademicTheme.ON_SURFACE);
        koleksiTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        koleksiTitle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                koleksiListExpanded = !koleksiListExpanded;
                refreshKoleksiList();
            }
        });
        JButton addBtn = new JButton("+");
        flatIconButton(addBtn, "Buat koleksi baru");
        addBtn.setForeground(AcademicTheme.TEXT_MUTED);
        addBtn.addActionListener(e -> createProjectFolder());
        koleksiHeader.add(koleksiTitle, BorderLayout.CENTER);
        koleksiHeader.add(addBtn, BorderLayout.EAST);
        koleksiSection.add(koleksiHeader, BorderLayout.NORTH);

        koleksiListPanel = new JPanel();
        koleksiListPanel.setOpaque(false);
        koleksiListPanel.setLayout(new BoxLayout(koleksiListPanel, BoxLayout.Y_AXIS));
        JScrollPane koleksiScroll = new JScrollPane(koleksiListPanel);
        koleksiScroll.setBorder(null);
        koleksiScroll.setOpaque(false);
        koleksiScroll.getViewport().setOpaque(false);
        koleksiScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        koleksiScroll.getVerticalScrollBar().setUnitIncrement(16);
        koleksiSection.add(koleksiScroll, BorderLayout.CENTER);

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

    /** Menampilkan/menyembunyikan sidebar kiri dan menyesuaikan ulang tata letak. */
    private void toggleSidebar() {
        boolean visible = !sidebar.isVisible();
        sidebar.setVisible(visible);
        brandBox.setPreferredSize(new Dimension(visible ? SIDEBAR_WIDTH - 14 : 0, 1));
        headerToolbar.revalidate();
        layoutContentLayer();
    }

    /** Baris navigasi (Hasil Pencarian / Favorit) dengan sudut membulat & hover. */
    private RoundedPanel createNavRow(JLabel label, JComponent trailing, Runnable action) {
        RoundedPanel row = new RoundedPanel(10, AcademicTheme.SURFACE_LOW, AcademicTheme.SURFACE_CONTAINER);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(label, BorderLayout.CENTER);
        if (trailing != null) {
            row.add(trailing, BorderLayout.EAST);
        }
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return row;
    }

    /** Mewarnai baris nav sesuai view yang sedang aktif. */
    private void refreshNavHighlight() {
        if (navSearchRow == null || navFavoritesRow == null) return;
        boolean searchActive = activeView == SidebarView.SEARCH_RESULTS;
        boolean favActive = activeView == SidebarView.COLLECTION
                && workspace.activeProject().equals(workspace.favoritesPath());

        applyRowStyle(navSearchRow, navSearchLabel, AcademicIcons.search(
                searchActive ? Color.WHITE : AcademicTheme.ON_SURFACE_VARIANT, 15), searchActive);
        applyRowStyle(navFavoritesRow, navFavoritesLabel, AcademicIcons.star(
                favActive ? Color.WHITE : AcademicTheme.ON_SURFACE_VARIANT, 15), favActive);

        if (navFavoritesCount != null) {
            int favCount = workspace.articleCount(workspace.favoritesPath());
            navFavoritesCount.setText(String.valueOf(favCount));
            navFavoritesCount.setBackground(favActive ? new Color(255, 255, 255, 50) : AcademicTheme.SURFACE_HIGH);
            navFavoritesCount.setForeground(favActive ? Color.WHITE : AcademicTheme.ON_SURFACE);
        }
    }

    private void applyRowStyle(RoundedPanel row, JLabel label, Icon icon, boolean active) {
        row.setActive(active);
        row.setBackground(active ? AcademicTheme.PRIMARY : AcademicTheme.SURFACE_LOW);
        label.setIcon(icon);
        label.setIconTextGap(8);
        label.setFont(AcademicTheme.uiFont(active ? Font.BOLD : Font.PLAIN, 13f));
        label.setForeground(active ? Color.WHITE : AcademicTheme.ON_SURFACE_VARIANT);
    }

    /** Menyusun ulang daftar koleksi (folder di projects/) dengan koleksi aktif disorot. */
    private void refreshKoleksiList() {
        if (koleksiListPanel == null) return;
        koleksiListPanel.removeAll();
        if (koleksiListExpanded) {
            List<Path> collections = workspace.sidebarCollections();
            if (collections.isEmpty()) {
                JLabel empty = new JLabel("Belum ada koleksi");
                empty.setFont(AcademicTheme.uiFont(Font.ITALIC, 11.5f));
                empty.setForeground(AcademicTheme.TEXT_MUTED);
                empty.setBorder(new EmptyBorder(4, 10, 4, 4));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                koleksiListPanel.add(empty);
            }
            for (Path path : collections) {
                koleksiListPanel.add(createKoleksiRow(path, isCollectionInView(path)));
            }
        }
        koleksiListPanel.revalidate();
        koleksiListPanel.repaint();
    }

    /** Baris koleksi: klik = muat artikel koleksi, klik kanan = hapus. */
    private JPanel createKoleksiRow(Path path, boolean active) {
        int count = workspace.articleCount(path);
        RoundedPanel row = new RoundedPanel(8, AcademicTheme.SURFACE_LOW, AcademicTheme.SURFACE_CONTAINER);
        row.setLayout(new BorderLayout(8, 0));
        row.setActive(active);
        row.setBorder(new EmptyBorder(6, 10, 6, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setToolTipText(path.toString());
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setBackground(active ? AcademicTheme.PRIMARY : AcademicTheme.SURFACE_LOW);

        JLabel nameLabel = new JLabel(workspace.displayName(path),
                AcademicIcons.folder(active ? Color.WHITE : AcademicTheme.TEXT_MUTED, 15),
                SwingConstants.LEFT);
        nameLabel.setIconTextGap(8);
        nameLabel.setFont(AcademicTheme.uiFont(active ? Font.BOLD : Font.PLAIN, 13f));
        nameLabel.setForeground(active ? Color.WHITE : AcademicTheme.ON_SURFACE_VARIANT);

        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(AcademicTheme.monoFont(Font.PLAIN, 11.5f));
        countLbl.setForeground(active ? Color.WHITE : AcademicTheme.TEXT_MUTED);

        row.add(nameLabel, BorderLayout.CENTER);
        row.add(countLbl, BorderLayout.EAST);
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showCollectionContextMenu(e, path, row);
                } else {
                    openCollection(path);
                }
            }
        });
        return row;
    }

    private boolean isCollectionInView(Path path) {
        return activeView == SidebarView.COLLECTION
                && workspace.activeProject().equals(path.toAbsolutePath().normalize());
    }

    private void showCollectionContextMenu(MouseEvent e, Path path, Component row) {
        if (!workspace.canDelete(path)) return;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Hapus Koleksi...");
        deleteItem.addActionListener(ev -> deleteCollection(path));
        menu.add(deleteItem);
        menu.show(row, e.getX(), e.getY());
    }

    /** Menghapus folder koleksi secara rekursif setelah konfirmasi pengguna. */
    private void deleteCollection(Path path) {
        if (!workspace.canDelete(path)) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hapus koleksi \"" + workspace.displayName(path) + "\" beserta seluruh isinya?",
                "Hapus Koleksi", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        boolean wasActive = isCollectionInView(path);
        try {
            workspace.deleteCollection(path);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus koleksi: " + ex.getMessage(),
                    "Hapus Koleksi", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Buat Koleksi", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal membuat koleksi: " + ex.getMessage(),
                    "Buat Koleksi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Search Card terpadu: baris 1 (Kata kunci + Cari + Pencarian Lanjutan), baris 2 (Jurnal + Penulis), dan drawer lanjutan. */
    private JPanel createSearchBar(JButton advancedSearchButton) {
        AcademicTheme.styleInput(keywordField, "Cari artikel dengan kata kunci... (contoh: machine learning, hukum adat)");
        keywordField.setPreferredSize(new Dimension(260, 36));
        keywordField.putClientProperty("JTextField.showClearButton", true);

        AcademicTheme.styleInput(advancedJournalField, "Dalam judul jurnal atau buku...");
        advancedJournalField.setPreferredSize(new Dimension(200, 34));
        advancedJournalField.putClientProperty("JTextField.leadingIcon", AcademicIcons.book(AcademicTheme.TEXT_MUTED, 15));

        AcademicTheme.styleInput(advancedAuthorField, "Nama penulis (contoh: B. Suhartono, Widyaningsih)...");
        advancedAuthorField.setPreferredSize(new Dimension(200, 34));
        advancedAuthorField.putClientProperty("JTextField.leadingIcon", AcademicIcons.person(AcademicTheme.TEXT_MUTED, 15));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AcademicTheme.CARD_BG);
        card.setBorder(AcademicTheme.cardBorder(12));

        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);
        row1.add(keywordField, BorderLayout.CENTER);
        JPanel row1Btns = new JPanel();
        row1Btns.setOpaque(false);
        row1Btns.setLayout(new BoxLayout(row1Btns, BoxLayout.X_AXIS));
        row1Btns.add(crawlBtn);
        row1Btns.add(Box.createHorizontalStrut(8));
        row1Btns.add(advancedSearchButton);
        row1.add(row1Btns, BorderLayout.EAST);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
        row2.setOpaque(false);
        row2.add(advancedJournalField);
        row2.add(advancedAuthorField);

        card.add(row1);
        card.add(Box.createVerticalStrut(8));
        card.add(row2);
        card.add(advancedSearchPanel);
        return card;
    }

    /** Pasangan label kecil di atas komponen input. */
    private JPanel createLabeledSearchField(String label, JComponent field) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setOpaque(false);
        JLabel heading = new JLabel(label);
        heading.setFont(AcademicTheme.uiFont(Font.BOLD, 10.5f));
        heading.setForeground(AcademicTheme.TEXT_MUTED);
        heading.setHorizontalAlignment(JLabel.LEFT);
        box.add(heading, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    /** Drawer filter lanjutan 3 kolom di dalam Search Card (dipisah garis tipis 1px). */
    private JPanel createAdvancedSearchPanel() {
        AcademicTheme.styleInput(advancedAffiliationField, "Semua Universitas / Lembaga...");
        advancedAffiliationField.setPreferredSize(new Dimension(180, 32));
        advancedAffiliationField.putClientProperty("JTextField.leadingIcon", AcademicIcons.building(AcademicTheme.TEXT_MUTED, 14));

        AcademicTheme.styleInput(fromYearField, "2021");
        fromYearField.setHorizontalAlignment(JTextField.CENTER);
        fromYearField.setFont(AcademicTheme.monoFont(Font.PLAIN, 12.5f));
        fromYearField.setPreferredSize(new Dimension(64, 32));

        AcademicTheme.styleInput(toYearField, "2026");
        toYearField.setHorizontalAlignment(JTextField.CENTER);
        toYearField.setFont(AcademicTheme.monoFont(Font.PLAIN, 12.5f));
        toYearField.setPreferredSize(new Dimension(64, 32));

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(10, 0, 0, 0));

        JSeparator divider = new JSeparator();
        divider.setForeground(AcademicTheme.BORDER_SUBTLE);
        wrapper.add(divider, BorderLayout.NORTH);

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridx = 0;
        gbc.weightx = 0.36;
        gbc.insets = new Insets(0, 0, 0, 12);
        inner.add(createLabeledSearchField("AFILIASI / INSTITUSI", advancedAffiliationField), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.36;
        gbc.insets = new Insets(0, 0, 0, 12);
        inner.add(createLabeledSearchField("TINGKAT AKREDITASI", sintaComboBox), gbc);

        JPanel yearRange = new JPanel(new GridBagLayout());
        yearRange.setOpaque(false);
        GridBagConstraints yc = new GridBagConstraints();
        yc.gridy = 0;
        yc.fill = GridBagConstraints.HORIZONTAL;
        yc.gridx = 0; yc.weightx = 1.0;
        yearRange.add(fromYearField, yc);
        yc.gridx = 1; yc.weightx = 0; yc.insets = new Insets(0, 6, 0, 6);
        yearRange.add(createInlineLabel("hingga"), yc);
        yc.gridx = 2; yc.weightx = 1.0; yc.insets = new Insets(0, 0, 0, 0);
        yearRange.add(toYearField, yc);

        gbc.gridx = 2;
        gbc.weightx = 0.28;
        gbc.insets = new Insets(0, 0, 0, 0);
        inner.add(createLabeledSearchField("RENTANG TAHUN TERBIT", yearRange), gbc);

        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel createInlineLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AcademicTheme.monoFont(Font.PLAIN, 11.5f));
        label.setForeground(AcademicTheme.TEXT_MUTED);
        return label;
    }

    private void layoutContentLayer() {
        int headerHeight = headerToolbar.isVisible() ? headerToolbar.getPreferredSize().height : 0;
        int contentLeft = sidebar.isVisible() ? SIDEBAR_WIDTH : 0;

        headerToolbar.setBounds(0, 0, contentLayer.getWidth(), headerHeight);

        // Tombol toggle mengambang di bilah judul: kiri tiga tombol jendela (±132px)
        if (sidebarToggleBtn != null) {
            int toggleX = Math.max(0, getRootPane().getWidth() - 3 * 44 - 8
                    - sidebarToggleBtn.getPreferredSize().width);
            sidebarToggleBtn.setBounds(toggleX, 3, sidebarToggleBtn.getPreferredSize().width, 26);
        }

        if (sidebar.isVisible()) {
            sidebar.setBounds(0, 0, SIDEBAR_WIDTH, contentLayer.getHeight());
        }

        mainSplit.setBounds(contentLeft, headerHeight,
                Math.max(0, contentLayer.getWidth() - contentLeft),
                Math.max(0, contentLayer.getHeight() - headerHeight));
        headerToolbar.revalidate();
        headerToolbar.repaint();
    }

    /** Inspector hanya muncul saat ada artikel yang dipilih di tabel. */
    private void showArticleInInspector(Article article) {
        inspectorPanel.setArticle(article);
        boolean show = article != null;
        boolean wasVisible = inspectorPanel.isVisible();
        if (!show) {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, inspectorPanel)) {
                clearInputFocus();
            }
        }
        inspectorPanel.setVisible(show);
        mainSplit.setDividerSize(show ? 6 : 0);
        if (show && !wasVisible) {
            // JSplitPane memberi seluruh ruang ke tabel saat inspector tidak visible;
            // saat muncul kembali, kembalikan proporsi 68 : 32
            mainSplit.setDividerLocation(0.68);
        }
    }

    /** Toggle baris filter lanjutan: memunculkan/menyembunyikan drawer di dalam Search Card. */
    private void toggleAdvancedSearch(JButton anchor) {
        boolean visible = !advancedSearchPanel.isVisible();
        if (!visible) {
            clearInputFocus();
        }
        advancedSearchPanel.setVisible(visible);
        // Tinggi header berubah saat panel muncul/hilang → hitung ulang batas konten
        layoutContentLayer();
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
            JOptionPane.showMessageDialog(this,
                    "Ketikkan kata kunci pencarian terlebih dahulu (cth: 'pendidikan karakter', 'deep learning').",
                    "Validasi Kata Kunci", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer fromYear = null;
        Integer toYear = null;
        try {
            if (!fromYearField.getText().isBlank()) fromYear = Integer.parseInt(fromYearField.getText().strip());
            if (!toYearField.getText().isBlank()) toYear = Integer.parseInt(toYearField.getText().strip());
            if (fromYear != null && toYear != null && fromYear > toYear) {
                JOptionPane.showMessageDialog(this, "Tahun awal tidak boleh melebihi tahun akhir.",
                        "Validasi Tahun", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format tahun harus berupa angka.",
                    "Validasi Tahun", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SintaFilterItem selectedSinta = (SintaFilterItem) sintaComboBox.getSelectedItem();
        clearInputFocus();
        activeView = SidebarView.SEARCH_RESULTS;
        headerToolbar.setVisible(true);
        headerToolbar.revalidate();
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
            JOptionPane.showMessageDialog(this,
                    "Belum ada hasil pencarian untuk disimpan. Jalankan pencarian terlebih dahulu.",
                    "Simpan Hasil Pencarian", JOptionPane.WARNING_MESSAGE);
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
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Berkas \"" + target.getFileName() + "\" sudah ada. Timpa?",
                    "Simpan Hasil Pencarian", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        try {
            workspace.saveJson(target, articles);
            JOptionPane.showMessageDialog(this,
                    "Berhasil menyimpan " + articles.size() + " artikel ke:\n" + target,
                    "Simpan Hasil Pencarian", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + ex.getMessage(),
                    "Simpan Hasil Pencarian", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelRunningSearch() {
        searchController.cancel();
        setSearchRunning(false, null);
    }

    /** Menampilkan kembali hasil pencarian terakhir. */
    private void showSearchResults() {
        cancelRunningSearch();
        clearInputFocus();
        tablePanel.clearSelection();
        showArticleInInspector(null);
        activeView = SidebarView.SEARCH_RESULTS;
        refreshNavHighlight();
        refreshKoleksiList();
        headerToolbar.setVisible(true);
        headerToolbar.revalidate();
        layoutContentLayer();
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
            JOptionPane.showMessageDialog(this, "Gagal menambahkan ke koleksi: " + ex.getMessage(),
                    "Tambah ke Koleksi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void showAddToCollectionDialog(Article article) {
        List<Path> collections = workspace.userCollections();
        if (collections.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Belum ada koleksi. Buat dulu lewat tombol + di sidebar.",
                    "Tambah ke Koleksi", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this,
                    "Artikel ditambahkan ke koleksi \"" + workspace.displayName(target) + "\".",
                    "Tambah ke Koleksi", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Artikel sudah ada di koleksi tersebut.",
                    "Tambah ke Koleksi", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openCollection(Path path) {
        cancelRunningSearch();
        clearInputFocus();
        tablePanel.clearSelection();
        showArticleInInspector(null);
        activeView = SidebarView.COLLECTION;
        setActiveProject(path);
        refreshNavHighlight();
        headerToolbar.setVisible(false);
        if (advancedSearchPanel.isVisible()) toggleAdvancedSearch(advancedSearchButton);

        List<Article> saved = workspace.loadCollection(path);
        tablePanel.setArticles(saved);
        tablePanel.updatePagination(1, 1, saved.size(), saved.size());
        tablePanel.setProgress(false, saved.isEmpty()
                ? "Koleksi \"" + workspace.displayName(path) + "\" masih kosong."
                : "");
        tablePanel.setBrowseMode(true);
        layoutContentLayer();
    }

    private void removeFromActiveCollection(Article article) {
        if (activeView != SidebarView.COLLECTION) return;
        Path collection = workspace.activeProject();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Buang artikel ini dari koleksi \"" + workspace.displayName(collection) + "\"?",
                "Buang dari Koleksi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            List<Article> existing = workspace.removeFromCollection(collection, article);
            tablePanel.setArticles(existing);
            tablePanel.updatePagination(1, 1, existing.size(), existing.size());
            refreshKoleksiList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal membuang artikel: " + ex.getMessage(),
                    "Buang dari Koleksi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                AcademicTheme.applyGlobalDefaults();
            } catch (Exception ignored) {}
            new GoCrawlGui().setVisible(true);
        });
    }

    /** Panel dengan sudut membulat anti-aliased dan efek hover halus untuk item sidebar. */
    private static final class RoundedPanel extends JPanel {
        private final int arc;
        private final Color hoverBg;
        private boolean hovered = false;
        private boolean active = false;

        RoundedPanel(int arc, Color defaultBg, Color hoverBg) {
            this.arc = arc;
            this.hoverBg = hoverBg;
            setOpaque(false);
            setBackground(defaultBg);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor((!active && hovered) ? hoverBg : getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Label berbentuk pill bundar penuh untuk badge jumlah artikel di sidebar. */
    private static final class PillLabel extends JLabel {
        PillLabel(String text) {
            super(text, SwingConstants.CENTER);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            int h = getHeight();
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
