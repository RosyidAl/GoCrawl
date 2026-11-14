package id.gocrawl.gui;

import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog "Kelola Sumber Crawl": daftar sumber pencarian dengan checkbox aktif,
 * tambah sumber kustom (Nama + Base URL, diasumsikan kompatibel Garuda),
 * dan hapus sumber kustom. Garuda = bawaan, tidak dapat dihapus.
 * Setiap perubahan langsung disimpan ke data/crawl_sources.txt.
 */
public class CrawlSourcesDialog extends JDialog {

    private final List<CrawlSource> sources;
    private final Path storeFile;
    private final Runnable onSourcesChanged;
    private JPanel rowsPanel;

    public CrawlSourcesDialog(Frame owner, List<CrawlSource> sources, Path storeFile, Runnable onSourcesChanged) {
        super(owner, "Kelola Sumber Crawl", true);
        this.sources = sources;
        this.storeFile = storeFile;
        this.onSourcesChanged = onSourcesChanged;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 12, 14));

        JLabel hint = new JLabel("Centang sumber yang dipakai saat crawl. Pencarian memakai sumber aktif pertama.");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(new Color(110, 110, 110));
        add(hint, BorderLayout.NORTH);

        rowsPanel = new JPanel();
        rowsPanel.setOpaque(false);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(rowsPanel);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(480, 160));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton addBtn = new JButton("＋ Tambah Sumber");
        addBtn.addActionListener(e -> showAddSourceDialog());
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addWrap.setOpaque(false);
        addWrap.add(addBtn);
        bottom.add(addWrap, BorderLayout.WEST);

        JButton closeBtn = new JButton("Tutup");
        closeBtn.addActionListener(e -> dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(closeBtn);
        bottom.add(closeWrap, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        renderRows();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Menyusun ulang daftar baris sumber (checkbox + tombol hapus untuk kustom). */
    private void renderRows() {
        rowsPanel.removeAll();
        for (CrawlSource source : sources) {
            rowsPanel.add(createSourceRow(source));
        }
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private JPanel createSourceRow(CrawlSource source) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));

        JCheckBox checkbox = new JCheckBox(
                source.name() + " — " + source.baseUrl() + (source.builtin() ? "   [bawaan]" : ""),
                source.enabled());
        checkbox.setOpaque(false);
        checkbox.addActionListener(e -> {
            updateSource(source, checkbox.isSelected());
        });
        row.add(checkbox, BorderLayout.CENTER);

        if (!source.builtin()) {
            JButton deleteBtn = new JButton("Hapus");
            deleteBtn.setFont(deleteBtn.getFont().deriveFont(11f));
            deleteBtn.setMargin(new Insets(2, 8, 2, 8));
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Hapus sumber \"" + source.name() + "\"?",
                        "Hapus Sumber", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                sources.remove(source);
                persist();
                renderRows();
            });
            row.add(deleteBtn, BorderLayout.EAST);
        }
        return row;
    }

    /** Form kecil tambah sumber kustom: Nama + Base URL. */
    private void showAddSourceDialog() {
        JTextField nameField = new JTextField();
        JTextField urlField = new JTextField("https://");
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel fields = new JPanel(new GridLayout(2, 2, 6, 6));
        fields.add(new JLabel("Nama sumber:"));
        fields.add(nameField);
        fields.add(new JLabel("Base URL:"));
        fields.add(urlField);
        panel.add(new JLabel("Sumber kustom diasumsikan kompatibel Garuda (URL pencarian: baseUrl/documents?q=...):"), BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Tambah Sumber",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().strip();
        String baseUrl = urlField.getText().strip();
        if (name.isEmpty() || name.contains("|")) {
            JOptionPane.showMessageDialog(this, "Nama sumber tidak boleh kosong.", "Tambah Sumber", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            JOptionPane.showMessageDialog(this, "Base URL harus dimulai dengan http:// atau https://", "Tambah Sumber", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        sources.add(new CrawlSource(name, baseUrl, true, false));
        persist();
        renderRows();
    }

    private void updateSource(CrawlSource source, boolean enabled) {
        int idx = sources.indexOf(source);
        if (idx >= 0) {
            sources.set(idx, new CrawlSource(source.name(), source.baseUrl(), enabled, source.builtin()));
        }
        persist();
    }

    /** Simpan daftar terkini + beri tahu pemilik dialog bahwa sumber berubah. */
    private void persist() {
        try {
            CrawlSourceStore.save(storeFile, new ArrayList<>(sources));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan daftar sumber: " + ex.getMessage(),
                    "Kelola Sumber Crawl", JOptionPane.ERROR_MESSAGE);
        }
        if (onSourcesChanged != null) {
            onSourcesChanged.run();
        }
    }
}
