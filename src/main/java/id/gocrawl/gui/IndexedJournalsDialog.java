package id.gocrawl.gui;

import id.gocrawl.GarudaSubject;
import id.gocrawl.Journal;
import id.gocrawl.JournalCatalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialog untuk melihat, menyaring, dan mengelola seluruh daftar jurnal ilmiah
 * yang telah terdaftar di dalam Master Journal Registry lokal (data/journals_master.json).
 * Dilengkapi tag/lingkup bidang ilmu resmi Portal Garuda dan filter multi-kriteria.
 */
public class IndexedJournalsDialog extends JDialog {

    private final String[] COLUMN_NAMES = {
            "Nama Jurnal",
            "Akreditasi SINTA",
            "Tag / Lingkup Garuda",
            "Penerbit / Institusi",
            "ISSN",
            "E-ISSN",
            "Tautan OJS"
    };

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> sintaFilterCombo = new JComboBox<>(new String[]{"Semua SINTA", "S1", "S2", "S3", "S4", "S5", "S6"});
    private final JComboBox<String> scopeFilterCombo = new JComboBox<>();
    private final JLabel countLabel = new JLabel("Total Jurnal Terdaftar: 0");

    private JournalCatalog catalog;
    private final Runnable onCatalogUpdated;

    public IndexedJournalsDialog(Frame parent, JournalCatalog catalog, Runnable onCatalogUpdated) {
        super(parent, "Katalog Jurnal Terindeks (Master Registry)", true);
        this.catalog = catalog;
        this.onCatalogUpdated = onCatalogUpdated;

        initUI();
        loadJournals();
    }

    private void initUI() {
        setSize(1060, 580);
        setMinimumSize(new Dimension(860, 440));
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(0, 10));

        // 1. Toolbar Atas (Pencarian, Filter SINTA, & Filter Lingkup Garuda)
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(new EmptyBorder(12, 14, 4, 14));

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBox.add(new JLabel("Cari Jurnal:"));
        searchField.setToolTipText("Ketik nama jurnal, penerbit, tag lingkup, atau nomor ISSN");
        filterBox.add(searchField);

        filterBox.add(Box.createHorizontalStrut(8));
        filterBox.add(new JLabel("Peringkat:"));
        filterBox.add(sintaFilterCombo);

        filterBox.add(Box.createHorizontalStrut(8));
        filterBox.add(new JLabel("Lingkup:"));
        scopeFilterCombo.addItem("Semua Lingkup");
        filterBox.add(scopeFilterCombo);

        topPanel.add(filterBox, BorderLayout.WEST);

        JButton addBtn = new JButton("+ Tambah Jurnal");
        addBtn.setFont(addBtn.getFont().deriveFont(Font.BOLD, 12f));
        addBtn.addActionListener(e -> {
            new AddJournalDialog(this, () -> {
                try {
                    this.catalog = JournalCatalog.load(Path.of("data", "journals_master.json"));
                    loadJournals();
                    if (onCatalogUpdated != null) onCatalogUpdated.run();
                } catch (Exception ignored) {}
            }).setVisible(true);
        });
        topPanel.add(addBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Tabel Jurnal
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(0).setPreferredWidth(250); // Nama Jurnal
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // SINTA
        table.getColumnModel().getColumn(2).setPreferredWidth(210); // Tag / Lingkup Garuda
        table.getColumnModel().getColumn(3).setPreferredWidth(200); // Penerbit
        table.getColumnModel().getColumn(4).setPreferredWidth(85);  // ISSN
        table.getColumnModel().getColumn(5).setPreferredWidth(85);  // E-ISSN
        table.getColumnModel().getColumn(6).setPreferredWidth(160); // OJS URL

        // Center Alignment & Styling
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer sintaRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (!isSelected) {
                    String s = value != null ? value.toString() : "-";
                    if (s.startsWith("S") || s.contains("Scopus")) {
                        setForeground(new Color(180, 80, 20));
                        setFont(getFont().deriveFont(Font.BOLD, 12f));
                    } else {
                        setForeground(Color.GRAY);
                    }
                }
                return c;
            }
        };

        DefaultTableCellRenderer tagRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String s = value != null ? value.toString() : "-";
                    if (!s.equals("-")) {
                        setForeground(new Color(30, 115, 60)); // Green badge
                        setFont(getFont().deriveFont(Font.BOLD, 11.5f));
                    } else {
                        setForeground(Color.GRAY);
                    }
                }
                return c;
            }
        };

        table.getColumnModel().getColumn(1).setCellRenderer(sintaRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(tagRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 14, 0, 14),
                scrollPane.getBorder()
        ));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(new EmptyBorder(8, 14, 12, 14));

        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 12f));
        bottomPanel.add(countLabel, BorderLayout.WEST);

        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton openWebBtn = new JButton("Buka Web Jurnal di Browser");
        JButton closeBtn = new JButton("Tutup");

        openWebBtn.addActionListener(e -> openSelectedJournal());
        closeBtn.addActionListener(e -> dispose());

        actionBox.add(openWebBtn);
        actionBox.add(closeBtn);
        bottomPanel.add(actionBox, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Event Handlers
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        sintaFilterCombo.addActionListener(e -> applyFilters());
        scopeFilterCombo.addActionListener(e -> applyFilters());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedJournal();
                }
            }
        });
    }

    private void loadJournals() {
        tableModel.setRowCount(0);
        if (catalog == null) return;

        List<Journal> journals = catalog.allJournals();
        Set<String> scopesInCatalog = new TreeSet<>();

        for (Journal j : journals) {
            String sinta = j.sintaRank() != null && !j.sintaRank().isBlank() ? j.sintaRank() : "-";
            String tag = resolveSubjectTag(j);
            if (!tag.equals("-")) {
                scopesInCatalog.add(tag);
            }

            tableModel.addRow(new Object[]{
                    j.name(),
                    sinta,
                    tag,
                    j.publisher() != null ? j.publisher() : "-",
                    j.issn() != null ? j.issn() : "-",
                    j.eissn() != null ? j.eissn() : "-",
                    j.ojsUrl() != null ? j.ojsUrl() : "-"
            });
        }

        // Update scope filter items without triggering unnecessary reset
        String prevScope = (String) scopeFilterCombo.getSelectedItem();
        scopeFilterCombo.removeAllItems();
        scopeFilterCombo.addItem("Semua Lingkup");
        for (String scope : scopesInCatalog) {
            scopeFilterCombo.addItem(scope);
        }
        if (prevScope != null && scopesInCatalog.contains(prevScope)) {
            scopeFilterCombo.setSelectedItem(prevScope);
        }

        applyFilters();
    }

    private static String resolveSubjectTag(Journal j) {
        if (j == null) return "-";
        String subId = j.subjectId();
        if ((subId == null || subId.isBlank() || subId.equals("-")) && j.subjectName() != null && !j.subjectName().isBlank()) {
            subId = GarudaSubject.resolveSubjectId(j.subjectName());
        }
        if (subId != null && !subId.isBlank() && !subId.equals("-")) {
            String label = GarudaSubject.getDisplayLabel(subId);
            if (label != null && !label.equals("Unknown Subject")) {
                return label;
            }
        }
        if (j.subjectName() != null && !j.subjectName().isBlank() && !j.subjectName().equals("-")) {
            return j.subjectName();
        }
        return "-";
    }

    private void applyFilters() {
        String query = searchField.getText().strip().toLowerCase(Locale.ROOT);
        String sintaChoice = (String) sintaFilterCombo.getSelectedItem();
        final boolean filterSinta = sintaChoice != null && !sintaChoice.startsWith("Semua");

        String scopeChoice = (String) scopeFilterCombo.getSelectedItem();
        final boolean filterScope = scopeChoice != null && !scopeChoice.startsWith("Semua");

        RowFilter<DefaultTableModel, Integer> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (filterSinta) {
                    String sintaVal = entry.getStringValue(1);
                    if (!sintaVal.equalsIgnoreCase(sintaChoice)) {
                        return false;
                    }
                }
                if (filterScope) {
                    String scopeVal = entry.getStringValue(2);
                    if (!scopeVal.equalsIgnoreCase(scopeChoice)) {
                        return false;
                    }
                }
                if (!query.isEmpty()) {
                    boolean textMatched = false;
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        if (entry.getStringValue(i).toLowerCase(Locale.ROOT).contains(query)) {
                            textMatched = true;
                            break;
                        }
                    }
                    if (!textMatched) return false;
                }
                return true;
            }
        };

        sorter.setRowFilter(combinedFilter);
        int visibleCount = table.getRowCount();
        int totalCount = tableModel.getRowCount();
        if (visibleCount == totalCount) {
            countLabel.setText("Total Jurnal Terdaftar: " + totalCount);
        } else {
            countLabel.setText("Menampilkan " + visibleCount + " dari " + totalCount + " jurnal");
        }
    }

    private void openSelectedJournal() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu jurnal dari tabel terlebih dahulu.", "Buka Jurnal", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String url = (String) tableModel.getValueAt(modelRow, 6);
        if (url != null && !url.isBlank() && !url.equals("-")) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal membuka browser: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Tautan OJS tidak tersedia untuk jurnal ini.", "Buka Jurnal", JOptionPane.WARNING_MESSAGE);
        }
    }
}
