package id.gocrawl.gui;

import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog "Kelola Sumber Crawl": daftar sumber pencarian dengan checkbox aktif,
 * tambah sumber kustom lewat form URL + Uji Koneksi &amp; Deteksi (meniru alur
 * Tambah Jurnal), dan hapus sumber kustom. Garuda = bawaan, tidak dapat dihapus.
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
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 16, 12, 16));

        JLabel hint = new JLabel("Centang sumber yang dipakai saat crawl — pencarian memakai sumber aktif pertama.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(new Color(120, 120, 120));
        add(hint, BorderLayout.NORTH);

        rowsPanel = new JPanel();
        rowsPanel.setOpaque(true);
        rowsPanel.setBackground(Color.WHITE);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(rowsPanel);
        scroll.setPreferredSize(new Dimension(500, 170));
        scroll.setBorder(new LineBorder(new Color(210, 214, 220), 1, true));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton addBtn = new JButton("＋ Tambah Sumber...");
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

        getRootPane().setDefaultButton(closeBtn);

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
        row.setBorder(new EmptyBorder(6, 2, 6, 2));

        JCheckBox checkbox = new JCheckBox(
                source.name() + " — " + source.baseUrl() + (source.builtin() ? "   [bawaan]" : ""),
                source.enabled());
        checkbox.setOpaque(false);
        checkbox.addActionListener(e -> updateSource(source, checkbox.isSelected()));
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

    /**
     * Form tambah sumber meniru alur "Tambah Jurnal": URL + Uji Koneksi &amp; Deteksi
     * yang menguji portal dan mengisi Nama Sumber otomatis.
     */
    private void showAddSourceDialog() {
        JTextField urlField = new JTextField("https://");
        JTextField nameField = new JTextField();
        JLabel statusLabel = new JLabel("Masukkan URL portal sumber di atas.");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setForeground(new Color(120, 120, 120));

        JButton testBtn = new JButton("Uji Koneksi & Deteksi");
        testBtn.addActionListener(e -> {
            String baseUrl = urlField.getText().strip();
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                statusLabel.setForeground(new Color(198, 40, 40));
                statusLabel.setText("URL harus dimulai dengan http:// atau https://");
                return;
            }
            String clean = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            testBtn.setEnabled(false);
            statusLabel.setForeground(new Color(110, 110, 110));
            statusLabel.setText("Menguji koneksi ke " + clean + " ...");
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    try {
                        Document doc = Jsoup.connect(clean + "/documents?q=test")
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl Source Tester")
                                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                                .timeout(15000)
                                .get();
                        String detected = doc.title();
                        return detected == null || detected.isBlank() ? "Sumber kustom" : detected.strip();
                    } catch (Exception ex) {
                        return null;
                    }
                }

                @Override
                protected void done() {
                    testBtn.setEnabled(true);
                    try {
                        String detected = get();
                        if (detected != null) {
                            statusLabel.setForeground(new Color(34, 139, 34));
                            statusLabel.setText("Terhubung — portal terdeteksi: " + detected);
                            nameField.setText(detected.length() > 40 ? detected.substring(0, 40).strip() : detected);
                        } else {
                            statusLabel.setForeground(new Color(198, 40, 40));
                            statusLabel.setText("Gagal terhubung (portal offline / tidak kompatibel Garuda).");
                        }
                    } catch (Exception ex) {
                        statusLabel.setForeground(new Color(198, 40, 40));
                        statusLabel.setText("Gagal terhubung: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        JPanel fields = new JPanel(new GridLayout(2, 2, 6, 6));
        fields.add(new JLabel("URL Sumber:"));
        JPanel urlRow = new JPanel(new BorderLayout(6, 0));
        urlRow.add(urlField, BorderLayout.CENTER);
        urlRow.add(testBtn, BorderLayout.EAST);
        fields.add(urlRow);
        fields.add(new JLabel("Nama Sumber:"));
        fields.add(nameField);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(statusLabel, BorderLayout.NORTH);
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
