package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.CitationExporter;
import id.gocrawl.JournalCatalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel Tengah: Menampilkan tabel daftar artikel dengan kolom Asal / Penerbit Jurnal yang jelas,
 * pencarian/penyaringan instan (real-time), bilah progres, serta tombol ekspor (CSV, JSON, BibTeX, RIS).
 */
public class ArticleTablePanel extends JPanel {

    private final String[] COLUMN_NAMES = {"Judul Artikel", "Jurnal", "Asal / Penerbit", "Penulis", "Tahun", "DOI"};
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField filterField = new JTextField(18);

    private final JLabel countLabel = new JLabel("Menampilkan: 0 artikel");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel progressLabel = new JLabel("");

    private final List<Article> allArticles = Collections.synchronizedList(new ArrayList<>());
    private final JournalCatalog catalog;
    private final Consumer<Article> onArticleSelected;

    public ArticleTablePanel(JournalCatalog catalog, Consumer<Article> onArticleSelected) {
        this.catalog = catalog;
        this.onArticleSelected = onArticleSelected;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // 1. Bilah Filter & Ekspor Atas
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filterBox.add(new JLabel("Saring Tabel:"));
        filterBox.add(filterField);
        topPanel.add(filterBox, BorderLayout.WEST);

        JPanel exportBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JButton exportCsvBtn = new JButton("Ekspor CSV");
        JButton exportJsonBtn = new JButton("Ekspor JSON");
        JButton exportBibBtn = new JButton("Ekspor BibTeX");
        JButton exportRisBtn = new JButton("Ekspor RIS");

        exportBox.add(exportCsvBtn);
        exportBox.add(exportJsonBtn);
        exportBox.add(exportBibBtn);
        exportBox.add(exportRisBtn);
        topPanel.add(exportBox, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Tabel Artikel
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setPreferredWidth(320); // Judul Artikel
        table.getColumnModel().getColumn(1).setPreferredWidth(170); // Jurnal
        table.getColumnModel().getColumn(2).setPreferredWidth(160); // Asal / Penerbit
        table.getColumnModel().getColumn(3).setPreferredWidth(130); // Penulis
        table.getColumnModel().getColumn(4).setPreferredWidth(55);  // Tahun
        table.getColumnModel().getColumn(5).setPreferredWidth(110); // DOI

        JScrollPane tableScroll = new JScrollPane(table);
        add(tableScroll, BorderLayout.CENTER);

        // 3. Bilah Status Bawah
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        bottomPanel.add(countLabel, BorderLayout.WEST);

        JPanel progressBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        progressLabel.setFont(progressLabel.getFont().deriveFont(11f));
        progressBar.setPreferredSize(new Dimension(160, 16));
        progressBar.setVisible(false);
        progressBox.add(progressLabel);
        progressBox.add(progressBar);
        bottomPanel.add(progressBox, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Instant filter as user types
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // Row Selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedViewRow = table.getSelectedRow();
                if (selectedViewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(selectedViewRow);
                    if (modelRow >= 0 && modelRow < allArticles.size()) {
                        Article a = allArticles.get(modelRow);
                        if (onArticleSelected != null) {
                            onArticleSelected.accept(a);
                        }
                    }
                } else {
                    if (onArticleSelected != null) {
                        onArticleSelected.accept(null);
                    }
                }
            }
        });

        // Right-Click Context Menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyTitleItem = new JMenuItem("Salin Judul");
        JMenuItem copyDoiItem = new JMenuItem("Salin DOI");
        JMenuItem copyBibItem = new JMenuItem("Salin Sitasi (BibTeX)");
        JMenuItem openBrowserItem = new JMenuItem("Buka Laman Artikel di Browser");

        contextMenu.add(copyTitleItem);
        contextMenu.add(copyDoiItem);
        contextMenu.add(copyBibItem);
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
        openBrowserItem.addActionListener(e -> {
            Article a = getSelectedArticle();
            if (a != null && a.url() != null && !a.url().isBlank()) {
                try {
                    Desktop.getDesktop().browse(new URI(a.url()));
                } catch (Exception ignored) {}
            }
        });

        // Export Actions
        exportCsvBtn.addActionListener(e -> exportData("csv", "Comma Separated Values (*.csv)"));
        exportJsonBtn.addActionListener(e -> exportData("json", "JSON Documents (*.json)"));
        exportBibBtn.addActionListener(e -> exportData("bib", "BibTeX References (*.bib)"));
        exportRisBtn.addActionListener(e -> exportData("ris", "RIS Citations (*.ris)"));
    }

    private void applyFilter() {
        String text = filterField.getText().strip();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
        updateCountLabel();
    }

    public synchronized void addArticle(Article article) {
        if (article == null) return;
        SwingUtilities.invokeLater(() -> {
            allArticles.add(article);
            String firstAuthor = article.authors() != null ? article.authors().split(";")[0].strip() : "-";
            String origin = catalog != null ? catalog.resolveOrigin(article.url(), article.journal()) : "-";
            tableModel.addRow(new Object[]{
                    article.title(),
                    article.journal(),
                    origin,
                    firstAuthor,
                    article.year(),
                    article.doi() == null || article.doi().isBlank() ? "-" : article.doi()
            });
            updateCountLabel();
        });
    }

    public synchronized void setArticles(List<Article> articles) {
        SwingUtilities.invokeLater(() -> {
            allArticles.clear();
            tableModel.setRowCount(0);
            if (articles != null) {
                allArticles.addAll(articles);
                for (Article a : articles) {
                    String firstAuthor = a.authors() != null ? a.authors().split(";")[0].strip() : "-";
                    String origin = catalog != null ? catalog.resolveOrigin(a.url(), a.journal()) : "-";
                    tableModel.addRow(new Object[]{
                            a.title(),
                            a.journal(),
                            origin,
                            firstAuthor,
                            a.year(),
                            a.doi() == null || a.doi().isBlank() ? "-" : a.doi()
                    });
                }
            }
            updateCountLabel();
        });
    }

    public void clearArticles() {
        SwingUtilities.invokeLater(() -> {
            allArticles.clear();
            tableModel.setRowCount(0);
            updateCountLabel();
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

    public List<Article> getAllArticles() {
        return new ArrayList<>(allArticles);
    }

    private void updateCountLabel() {
        int visible = table.getRowCount();
        int total = allArticles.size();
        if (visible == total) {
            countLabel.setText("Menampilkan: " + total + " artikel");
        } else {
            countLabel.setText("Menampilkan: " + visible + " dari " + total + " artikel");
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
                    case "csv" -> exportCsv(file);
                    case "json" -> exportJson(file);
                }
                JOptionPane.showMessageDialog(this, "Berhasil mengekspor " + allArticles.size() + " artikel ke " + file.getName(), "Ekspor Sukses", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mengekspor: " + ex.getMessage(), "Error Ekspor", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv(File file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("\"Title\",\"Journal\",\"Origin/Publisher\",\"Authors\",\"Year\",\"DOI\",\"URL\",\"Abstract\"\n");
            for (Article a : allArticles) {
                String origin = catalog != null ? catalog.resolveOrigin(a.url(), a.journal()) : "-";
                w.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCsv(a.title()),
                        escapeCsv(a.journal()),
                        escapeCsv(origin),
                        escapeCsv(a.authors()),
                        escapeCsv(a.year()),
                        escapeCsv(a.doi()),
                        escapeCsv(a.url()),
                        escapeCsv(a.abstractText())));
            }
        }
    }

    private void exportJson(File file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("[\n");
            for (int i = 0; i < allArticles.size(); i++) {
                Article a = allArticles.get(i);
                String origin = catalog != null ? catalog.resolveOrigin(a.url(), a.journal()) : "-";
                w.write("  {\n");
                w.write("    \"title\": \"" + escapeJson(a.title()) + "\",\n");
                w.write("    \"journal\": \"" + escapeJson(a.journal()) + "\",\n");
                w.write("    \"origin\": \"" + escapeJson(origin) + "\",\n");
                w.write("    \"authors\": \"" + escapeJson(a.authors()) + "\",\n");
                w.write("    \"year\": \"" + escapeJson(a.year()) + "\",\n");
                w.write("    \"doi\": \"" + escapeJson(a.doi()) + "\",\n");
                w.write("    \"url\": \"" + escapeJson(a.url()) + "\",\n");
                w.write("    \"abstract\": \"" + escapeJson(a.abstractText()) + "\"\n");
                w.write("  }" + (i < allArticles.size() - 1 ? ",\n" : "\n"));
            }
            w.write("]\n");
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private void copyToClipboard(String text) {
        if (text == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
