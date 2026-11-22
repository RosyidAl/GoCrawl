package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.CitationExporter;
import id.gocrawl.JournalCatalog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel Tengah: Menampilkan empty state eksplorasi atau tabel daftar artikel modern
 * dengan pill badge akreditasi SINTA, indikator seleksi 2px, filter real-time,
 * bilah progres, serta menu dropdown ekspor (CSV, JSON, BibTeX, RIS).
 */
public class ArticleTablePanel extends JPanel {

    private static final String CARD_EMPTY = "EMPTY";
    private static final String CARD_TABLE = "TABLE";

    private final String[] COLUMN_NAMES = {"Judul Artikel", "Jurnal", "Akreditasi", "Penerbit", "Penulis", "Tahun", "DOI"};
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField filterField = new JTextField(20);
    private final JButton clearFilterBtn = new JButton();

    private final CardLayout stageLayout = new CardLayout();
    private final JPanel stagePanel = new JPanel(stageLayout);
    private JPanel searchEmptyCard;
    private JPanel collectionEmptyCard;

    private final JLabel countLabel = new JLabel("0 artikel");
    private final JButton firstPageBtn = new JButton("«");
    private final JButton prevPageBtn = new JButton("‹");
    private final JTextField jumpPageField = new JTextField(3);
    private final JLabel pageTotalLabel = new JLabel("/ 1");
    private final JButton nextPageBtn = new JButton("›");
    private final JButton lastPageBtn = new JButton("»");
    private final JComboBox<Integer> pageSizeComboBox = new JComboBox<>(new Integer[]{10, 20, 25, 50});

    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel progressLabel = new JLabel("");

    private final List<Article> allArticles = Collections.synchronizedList(new ArrayList<>());
    private final JournalCatalog catalog;
    private final Consumer<Article> onArticleSelected;
    private Consumer<Integer> onPageChange;
    private Consumer<Integer> onPageSizeChange;
    private Consumer<Article> onAddToFavorites;
    private Consumer<Article> onRequestAddToCollection;
    private Consumer<Article> onRemoveFromCollection;
    private Consumer<String> onSampleKeywordSelected;
    private Runnable onFilterCleared;
    private boolean suppressSelectionEvents = false;
    private boolean browseMode = false;

    private int currentPage = 1;
    private int totalPages = 1;
    private int totalRecords = 0;

    public ArticleTablePanel(JournalCatalog catalog, Consumer<Article> onArticleSelected) {
        this.catalog = catalog;
        this.onArticleSelected = onArticleSelected;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));
        setBackground(AcademicTheme.SURFACE);
        setBorder(new EmptyBorder(4, 14, 8, 14));

        // 1. Bilah Filter & Dropdown Ekspor
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        AcademicTheme.styleInput(filterField, "Saring Tabel...");
        filterField.setBackground(AcademicTheme.CARD_BG);
        filterField.setPreferredSize(new Dimension(240, 32));
        filterField.putClientProperty("JTextField.leadingIcon", AcademicIcons.filter(AcademicTheme.TEXT_MUTED, 14));

        clearFilterBtn.setIcon(AcademicIcons.filterX(AcademicTheme.ON_SURFACE_VARIANT, 15));
        clearFilterBtn.setDisabledIcon(AcademicIcons.filterX(AcademicTheme.BORDER_VARIANT, 15));
        AcademicTheme.styleOutlineButton(clearFilterBtn);
        clearFilterBtn.setToolTipText("Buang saringan tabel");
        clearFilterBtn.setPreferredSize(new Dimension(32, 32));
        clearFilterBtn.setMargin(new Insets(0, 0, 0, 0));
        clearFilterBtn.setFocusable(false);
        clearFilterBtn.setEnabled(false);
        clearFilterBtn.addActionListener(e -> clearFilter());

        JPanel filterBox = new JPanel(new BorderLayout(6, 0));
        filterBox.setOpaque(false);
        filterBox.add(filterField, BorderLayout.CENTER);
        filterBox.add(clearFilterBtn, BorderLayout.EAST);
        topPanel.add(filterBox, BorderLayout.WEST);

        JButton exportMenuBtn = new JButton("Ekspor", AcademicIcons.download(AcademicTheme.ON_SURFACE_VARIANT, 14));
        AcademicTheme.styleOutlineButton(exportMenuBtn);
        exportMenuBtn.setPreferredSize(new Dimension(98, 32));
        JPopupMenu exportPopup = buildExportPopupMenu();
        exportMenuBtn.addActionListener(e -> exportPopup.show(exportMenuBtn, 0, exportMenuBtn.getHeight() + 4));

        JPanel exportBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        exportBox.setOpaque(false);
        exportBox.add(exportMenuBtn);
        topPanel.add(exportBox, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Stage Tengah (Empty State <-> Tabel)
        configureTable();
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(AcademicTheme.BORDER_SUBTLE, 1, true));
        tableScroll.getViewport().setBackground(AcademicTheme.CARD_BG);

        searchEmptyCard = createSearchEmptyState();
        collectionEmptyCard = createCollectionEmptyState();

        JPanel emptyContainer = new JPanel(new CardLayout());
        emptyContainer.setOpaque(false);
        emptyContainer.add(searchEmptyCard, "SEARCH");
        emptyContainer.add(collectionEmptyCard, "COLLECTION");

        stagePanel.setOpaque(false);
        stagePanel.add(emptyContainer, CARD_EMPTY);
        stagePanel.add(tableScroll, CARD_TABLE);
        add(stagePanel, BorderLayout.CENTER);

        // 3. Bilah Status & Paginasi Bawah
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(4, 2, 2, 2));

        countLabel.setFont(AcademicTheme.uiFont(Font.PLAIN, 12f));
        countLabel.setForeground(AcademicTheme.TEXT_MUTED);
        bottomPanel.add(countLabel, BorderLayout.WEST);

        JPanel centerBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        centerBox.setOpaque(false);

        stylePaginationButton(firstPageBtn, "Halaman Pertama", 30);
        firstPageBtn.addActionListener(e -> changePage(1));
        stylePaginationButton(prevPageBtn, "Halaman Sebelumnya", 30);
        prevPageBtn.addActionListener(e -> changePage(currentPage - 1));

        jumpPageField.setHorizontalAlignment(JTextField.CENTER);
        jumpPageField.setFont(AcademicTheme.monoFont(Font.PLAIN, 12f));
        jumpPageField.setPreferredSize(new Dimension(44, 28));
        jumpPageField.setToolTipText("Ketik nomor halaman lalu tekan Enter");
        jumpPageField.addActionListener(e -> triggerJump());

        pageTotalLabel.setFont(AcademicTheme.monoFont(Font.PLAIN, 12f));
        pageTotalLabel.setForeground(AcademicTheme.TEXT_MUTED);
        pageTotalLabel.setBorder(new EmptyBorder(0, 2, 0, 6));

        stylePaginationButton(nextPageBtn, "Halaman Berikutnya", 30);
        nextPageBtn.addActionListener(e -> changePage(currentPage + 1));
        stylePaginationButton(lastPageBtn, "Halaman Terakhir", 30);
        lastPageBtn.addActionListener(e -> changePage(totalPages));

        pageSizeComboBox.setPreferredSize(new Dimension(60, 28));
        pageSizeComboBox.setFont(AcademicTheme.monoFont(Font.PLAIN, 12f));
        pageSizeComboBox.setSelectedItem(25);
        pageSizeComboBox.setToolTipText("Artikel per halaman");
        pageSizeComboBox.addActionListener(e -> {
            if (onPageSizeChange != null) {
                onPageSizeChange.accept(getPageSize());
            }
        });

        centerBox.add(firstPageBtn);
        centerBox.add(prevPageBtn);
        centerBox.add(jumpPageField);
        centerBox.add(pageTotalLabel);
        centerBox.add(nextPageBtn);
        centerBox.add(lastPageBtn);
        centerBox.add(pageSizeComboBox);
        bottomPanel.add(centerBox, BorderLayout.CENTER);

        JPanel progressBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        progressBox.setOpaque(false);
        progressLabel.setFont(AcademicTheme.uiFont(Font.PLAIN, 11.5f));
        progressLabel.setForeground(AcademicTheme.ON_SURFACE_VARIANT);
        progressBar.setPreferredSize(new Dimension(150, 12));
        progressBar.setVisible(false);
        progressBox.add(progressLabel);
        progressBox.add(progressBar);
        bottomPanel.add(progressBox, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Instant filter
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // Row Selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (suppressSelectionEvents || e.getValueIsAdjusting()) {
                return;
            }
            int selectedViewRow = table.getSelectedRow();
            if (selectedViewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(selectedViewRow);
                if (modelRow >= 0 && modelRow < allArticles.size()) {
                    Article a = allArticles.get(modelRow);
                    if (onArticleSelected != null) {
                        onArticleSelected.accept(a);
                    }
                }
            } else if (onArticleSelected != null) {
                onArticleSelected.accept(null);
            }
        });

        setupContextMenu();
        updateStageVisibility();
    }

    private void configureTable() {
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setFont(AcademicTheme.uiFont(Font.PLAIN, 13f));
        table.setBackground(AcademicTheme.CARD_BG);
        table.setForeground(AcademicTheme.ON_SURFACE);
        table.setSelectionBackground(AcademicTheme.SELECTION_FILL);
        table.setSelectionForeground(AcademicTheme.ON_SURFACE);

        JTableHeader header = table.getTableHeader();
        header.setFont(AcademicTheme.uiFont(Font.BOLD, 11.5f));
        header.setBackground(AcademicTheme.SURFACE_LOW);
        header.setForeground(AcademicTheme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(320); // Judul Artikel
        table.getColumnModel().getColumn(1).setPreferredWidth(160); // Jurnal
        table.getColumnModel().getColumn(2).setPreferredWidth(85);  // Akreditasi
        table.getColumnModel().getColumn(3).setPreferredWidth(150); // Penerbit
        table.getColumnModel().getColumn(4).setPreferredWidth(120); // Penulis
        table.getColumnModel().getColumn(5).setPreferredWidth(62);  // Tahun
        table.getColumnModel().getColumn(6).setPreferredWidth(115); // DOI

        table.setDefaultRenderer(Object.class, new AcademicCellRenderer(false));
        table.getColumnModel().getColumn(2).setCellRenderer(new SintaBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new AcademicCellRenderer(true, SwingConstants.CENTER));
        table.getColumnModel().getColumn(6).setCellRenderer(new AcademicCellRenderer(true, SwingConstants.LEFT));
    }

    private JPopupMenu buildExportPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem csv = new JMenuItem("Format CSV (.csv)");
        JMenuItem json = new JMenuItem("Format JSON (.json)");
        JMenuItem bib = new JMenuItem("BibTeX (LaTeX) (.bib)");
        JMenuItem ris = new JMenuItem("Format RIS (EndNote) (.ris)");

        csv.addActionListener(e -> exportData("csv", "Comma Separated Values (*.csv)"));
        json.addActionListener(e -> exportData("json", "JSON Documents (*.json)"));
        bib.addActionListener(e -> exportData("bib", "BibTeX References (*.bib)"));
        ris.addActionListener(e -> exportData("ris", "RIS Citations (*.ris)"));

        menu.add(csv);
        menu.add(json);
        menu.add(bib);
        menu.add(ris);
        return menu;
    }

    private JPanel createSearchEmptyState() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(AcademicTheme.CARD_BG);
        card.setBorder(AcademicTheme.cardBorder(28));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setMaximumSize(new Dimension(560, 480));

        JLabel iconBox = new JLabel(AcademicIcons.emptyConstellation(68));
        iconBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconBox.setOpaque(true);
        iconBox.setBackground(AcademicTheme.SURFACE);
        iconBox.setBorder(new CompoundBorder(
                new LineBorder(AcademicTheme.BORDER_SUBTLE, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        center.add(iconBox);
        center.add(Box.createVerticalStrut(18));

        JLabel title = new JLabel("Mulai Eksplorasi Literatur Ilmiah");
        title.setFont(AcademicTheme.uiFont(Font.BOLD, 20f));
        title.setForeground(AcademicTheme.ON_SURFACE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);
        center.add(Box.createVerticalStrut(8));

        JLabel desc = new JLabel("<html><div style='text-align:center; width:430px;'>"
                + "Ketik kata kunci pencarian di atas untuk menelusuri puluhan ribu jurnal terindeks "
                + "di Indonesia seperti Garuda, Neliti, dan portal kampus.</div></html>");
        desc.setFont(AcademicTheme.uiFont(Font.PLAIN, 13.5f));
        desc.setForeground(AcademicTheme.ON_SURFACE_VARIANT);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(desc);
        center.add(Box.createVerticalStrut(22));

        JLabel chipHeading = new JLabel("CONTOH KATA KUNCI POPULER:");
        chipHeading.setFont(AcademicTheme.uiFont(Font.BOLD, 11f));
        chipHeading.setForeground(AcademicTheme.TEXT_MUTED);
        chipHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(chipHeading);
        center.add(Box.createVerticalStrut(8));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        chips.setOpaque(false);
        chips.setMaximumSize(new Dimension(540, 90));
        String[] samples = {
                "Model Chart Datum MLWS",
                "Hukum Perdata Islam",
                "Algoritma Genetika",
                "Analisis Sentimen Twitter"
        };
        for (String kw : samples) {
            JButton chip = new JButton(kw, AcademicIcons.arrowUpRight(AcademicTheme.TEXT_MUTED, 14));
            chip.setIconTextGap(6);
            chip.setFont(AcademicTheme.uiFont(Font.PLAIN, 12.5f));
            chip.setBackground(AcademicTheme.SURFACE_CONTAINER);
            chip.setForeground(AcademicTheme.ON_SURFACE);
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.putClientProperty("FlatLaf.style", "arc: 999; borderWidth: 0; hoverBackground: #DCE9FF");
            chip.addActionListener(e -> {
                if (onSampleKeywordSelected != null) {
                    onSampleKeywordSelected.accept(kw);
                }
            });
            chips.add(chip);
        }
        center.add(chips);
        center.add(Box.createVerticalStrut(20));

        JLabel trust = new JLabel("Sinta & Scopus Parsed   ·   DOI Resolution Support   ·   Realtime OJS Fetch");
        trust.setFont(AcademicTheme.monoFont(Font.PLAIN, 11.5f));
        trust.setForeground(AcademicTheme.TEXT_MUTED);
        trust.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(trust);

        card.add(center);
        return card;
    }

    private JPanel createCollectionEmptyState() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(AcademicTheme.CARD_BG);
        card.setBorder(AcademicTheme.cardBorder(24));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Koleksi Ini Masih Kosong");
        title.setFont(AcademicTheme.uiFont(Font.BOLD, 17f));
        title.setForeground(AcademicTheme.ON_SURFACE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);
        center.add(Box.createVerticalStrut(6));

        JLabel desc = new JLabel("Tambahkan artikel dari hasil pencarian melalui klik kanan pada baris tabel.");
        desc.setFont(AcademicTheme.uiFont(Font.PLAIN, 13f));
        desc.setForeground(AcademicTheme.ON_SURFACE_VARIANT);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(desc);

        card.add(center);
        return card;
    }

    private void stylePaginationButton(JButton btn, String tooltip, int width) {
        btn.setFont(AcademicTheme.uiFont(Font.BOLD, 13f));
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(width, 28));
        btn.setEnabled(false);
        btn.setFocusPainted(false);
        btn.putClientProperty("FlatLaf.style", "arc: 8");
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyTitleItem = new JMenuItem("Salin Judul");
        JMenuItem copyDoiItem = new JMenuItem("Salin DOI");
        JMenuItem copyBibItem = new JMenuItem("Salin Sitasi (BibTeX)");
        JMenuItem addToFavoritesItem = new JMenuItem("★ Tambahkan ke Favorit");
        JMenuItem addToCollectionItem = new JMenuItem("Tambahkan ke Koleksi...");
        JMenuItem removeFromCollectionItem = new JMenuItem("Buang dari Koleksi");
        JMenuItem openBrowserItem = new JMenuItem("Buka Laman Artikel di Browser");

        contextMenu.add(copyTitleItem);
        contextMenu.add(copyDoiItem);
        contextMenu.add(copyBibItem);
        contextMenu.addSeparator();
        contextMenu.add(addToFavoritesItem);
        contextMenu.add(addToCollectionItem);
        contextMenu.add(removeFromCollectionItem);
        contextMenu.addSeparator();
        contextMenu.add(openBrowserItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            private void showMenu(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) {
                    table.setRowSelectionInterval(r, r);
                    removeFromCollectionItem.setVisible(browseMode);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        copyTitleItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null) copyToClipboard(a.title());
        });
        copyDoiItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && a.doi() != null) copyToClipboard(a.doi());
        });
        copyBibItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null) copyToClipboard(CitationExporter.toBibTeX(List.of(a)));
        });
        addToFavoritesItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && onAddToFavorites != null) onAddToFavorites.accept(a);
        });
        addToCollectionItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && onRequestAddToCollection != null) onRequestAddToCollection.accept(a);
        });
        removeFromCollectionItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && onRemoveFromCollection != null) onRemoveFromCollection.accept(a);
        });
        openBrowserItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && a.url() != null && !a.url().isBlank()) {
                try {
                    Desktop.getDesktop().browse(new URI(a.url()));
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void updateStageVisibility() {
        if (allArticles.isEmpty()) {
            JPanel emptyContainer = (JPanel) stagePanel.getComponent(0);
            CardLayout cl = (CardLayout) emptyContainer.getLayout();
            cl.show(emptyContainer, browseMode ? "COLLECTION" : "SEARCH");
            stageLayout.show(stagePanel, CARD_EMPTY);
        } else {
            stageLayout.show(stagePanel, CARD_TABLE);
        }
    }

    private void applyFilter() {
        String text = filterField.getText().strip();
        suppressSelectionEvents = true;
        try {
            table.clearSelection();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
            }
        } finally {
            suppressSelectionEvents = false;
        }
        clearFilterBtn.setEnabled(!filterField.getText().isEmpty());
        updateCountLabel();
    }

    public void clearSelection() {
        suppressSelectionEvents = true;
        try {
            table.clearSelection();
        } finally {
            suppressSelectionEvents = false;
        }
    }

    public void clearFilter() {
        if (!filterField.getText().isEmpty()) {
            filterField.setText("");
        } else {
            applyFilter();
        }
        if (onFilterCleared != null) {
            onFilterCleared.run();
        }
    }

    public void setOnFilterCleared(Runnable callback) {
        this.onFilterCleared = callback;
    }

    JTextField filterField() {
        return filterField;
    }

    JButton clearFilterButton() {
        return clearFilterBtn;
    }

    JTable articleTable() {
        return table;
    }

    public synchronized void setArticles(List<Article> articles) {
        SwingUtilities.invokeLater(() -> {
            allArticles.clear();
            tableModel.setRowCount(0);
            if (articles != null) {
                allArticles.addAll(articles);
                articles.forEach(article -> tableModel.addRow(toTableRow(article)));
            }
            updateCountLabel();
            updateStageVisibility();
        });
    }

    private Object[] toTableRow(Article article) {
        String firstAuthor = article.authors() != null ? article.authors().split(";")[0].strip() : "-";
        String origin = catalog != null ? catalog.resolveOrigin(article.url(), article.journal()) : "-";
        String sinta = catalog != null ? catalog.resolveSintaRank(article.url(), article.journal()) : "-";
        return new Object[]{
                article.title(),
                article.journal(),
                sinta,
                origin,
                firstAuthor,
                article.year(),
                article.doi() == null || article.doi().isBlank() ? "-" : article.doi()
        };
    }

    private void triggerJump() {
        String text = jumpPageField.getText().strip();
        if (text.isEmpty()) return;
        try {
            int p = Integer.parseInt(text);
            if (p >= 1 && p <= Math.max(1, totalPages)) {
                changePage(p);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Nomor halaman harus di antara 1 dan " + Math.max(1, totalPages) + ".",
                        "Paginasi Halaman",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Masukkan angka nomor halaman yang valid.",
                    "Paginasi Halaman",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void changePage(int targetPage) {
        if (targetPage < 1 || (totalPages > 0 && targetPage > totalPages)) return;
        if (onPageChange != null) {
            onPageChange.accept(targetPage);
        }
    }

    public void updatePagination(int page, int totalPgs, int totalRecs, int currentCount) {
        this.currentPage = page;
        this.totalPages = Math.max(1, totalPgs);
        this.totalRecords = totalRecs;

        SwingUtilities.invokeLater(() -> {
            jumpPageField.setText(String.valueOf(currentPage));
            pageTotalLabel.setText("/ " + totalPages);
            firstPageBtn.setEnabled(currentPage > 1);
            prevPageBtn.setEnabled(currentPage > 1);
            nextPageBtn.setEnabled(currentPage < totalPages);
            lastPageBtn.setEnabled(currentPage < totalPages);
            countLabel.setText(currentCount + " artikel · Total " + totalRecs);
        });
    }

    public void setPaginationEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            firstPageBtn.setEnabled(enabled && currentPage > 1);
            prevPageBtn.setEnabled(enabled && currentPage > 1);
            nextPageBtn.setEnabled(enabled && currentPage < totalPages);
            lastPageBtn.setEnabled(enabled && currentPage < totalPages);
            jumpPageField.setEnabled(enabled);
            pageSizeComboBox.setEnabled(enabled);
        });
    }

    public void setOnPageChange(Consumer<Integer> callback) {
        this.onPageChange = callback;
    }

    public void setOnPageSizeChange(Consumer<Integer> callback) {
        this.onPageSizeChange = callback;
    }

    public void setOnAddToFavorites(Consumer<Article> callback) {
        this.onAddToFavorites = callback;
    }

    public void setOnAddToCollectionRequest(Consumer<Article> callback) {
        this.onRequestAddToCollection = callback;
    }

    public void setOnRemoveFromCollection(Consumer<Article> callback) {
        this.onRemoveFromCollection = callback;
    }

    public void setOnSampleKeywordSelected(Consumer<String> callback) {
        this.onSampleKeywordSelected = callback;
    }

    public void setBrowseMode(boolean browseMode) {
        this.browseMode = browseMode;
        updateStageVisibility();
    }

    public int getPageSize() {
        Object item = pageSizeComboBox.getSelectedItem();
        return item instanceof Integer ? (Integer) item : 25;
    }

    public void clearArticles() {
        SwingUtilities.invokeLater(() -> {
            allArticles.clear();
            tableModel.setRowCount(0);
            updateCountLabel();
            updateStageVisibility();
        });
    }

    public void setProgress(boolean active, String statusText) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(active);
            progressBar.setIndeterminate(active);
            progressLabel.setText(statusText);
        });
    }

    public Article getSelectedArticle() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow >= 0 && modelRow < allArticles.size()) {
            return allArticles.get(modelRow);
        }
        return null;
    }

    private void updateCountLabel() {
        int visible = table.getRowCount();
        int total = allArticles.size();
        if (visible == total) {
            countLabel.setText(total + " artikel");
        } else {
            countLabel.setText(visible + " dari " + total + " artikel");
        }
    }

    private void exportData(String ext, String desc) {
        if (allArticles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tidak ada data artikel untuk diekspor.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Ekspor Data (" + ext.toUpperCase() + ")");
        chooser.setSelectedFile(new File("gocrawl_articles." + ext));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                switch (ext.toLowerCase()) {
                    case "bib" -> CitationExporter.exportBibTeX(file.toPath(), allArticles);
                    case "ris" -> CitationExporter.exportRIS(file.toPath(), allArticles);
                    case "csv" -> ArticleExportService.export(file.toPath(), ArticleExportService.Format.CSV,
                            allArticles, catalog);
                    case "json" -> ArticleExportService.export(file.toPath(), ArticleExportService.Format.JSON,
                            allArticles, catalog);
                }
                JOptionPane.showMessageDialog(this, "Berhasil mengekspor " + allArticles.size() + " artikel ke " + file.getName(), "Ekspor Sukses", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mengekspor: " + ex.getMessage(), "Error Ekspor", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyToClipboard(String text) {
        if (text == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /** Renderer sel tabel dengan garis seleksi 2px biru di sisi kiri pada kolom pertama. */
    private static final class AcademicCellRenderer extends DefaultTableCellRenderer {
        private final boolean monospace;
        private final int align;

        AcademicCellRenderer(boolean monospace) {
            this(monospace, SwingConstants.LEFT);
        }

        AcademicCellRenderer(boolean monospace, int align) {
            this.monospace = monospace;
            this.align = align;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            lbl.setFont(monospace ? AcademicTheme.monoFont(Font.PLAIN, 12f) : AcademicTheme.uiFont(Font.PLAIN, 13f));
            lbl.setHorizontalAlignment(align);
            lbl.setBackground(isSelected ? AcademicTheme.SELECTION_FILL : AcademicTheme.CARD_BG);
            lbl.setForeground(AcademicTheme.ON_SURFACE);
            if (isSelected && column == 0) {
                lbl.setBorder(new CompoundBorder(
                        BorderFactory.createMatteBorder(0, 2, 0, 0, AcademicTheme.PRIMARY),
                        new EmptyBorder(0, 8, 0, 8)
                ));
            } else {
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
            }
            return lbl;
        }
    }

    /** Renderer pill badge berwarna untuk kolom akreditasi SINTA/Scopus. */
    private static final class SintaBadgeRenderer extends DefaultTableCellRenderer {
        private String rankText = "-";
        private boolean selectedRow = false;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            this.rankText = value == null || value.toString().isBlank() ? "-" : value.toString().trim().toUpperCase();
            this.selectedRow = isSelected;
            setOpaque(true);
            setBackground(isSelected ? AcademicTheme.SELECTION_FILL : AcademicTheme.CARD_BG);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selectedRow ? AcademicTheme.SELECTION_FILL : AcademicTheme.CARD_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Color[] colors = AcademicTheme.sintaBadgeColors(rankText);
            Font font = AcademicTheme.uiFont(Font.BOLD, 11f);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(rankText);
            int pillW = Math.max(30, textW + 16);
            int pillH = 20;
            int x = (getWidth() - pillW) / 2;
            int y = (getHeight() - pillH) / 2;

            g2.setColor(colors[1]);
            g2.fillRoundRect(x, y, pillW, pillH, pillH, pillH);
            g2.setColor(colors[2]);
            g2.drawRoundRect(x, y, pillW - 1, pillH - 1, pillH, pillH);

            g2.setColor(colors[0]);
            int tx = x + (pillW - textW) / 2;
            int ty = y + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(rankText, tx, ty);
            g2.dispose();
        }
    }
}
