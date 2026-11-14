package id.gocrawl.gui;

import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog "Kelola Sumber Crawl": satu jendela untuk semuanya — baris tambah cepat
 * (URL + Nama opsional) di atas, daftar sumber dengan checkbox aktif di bawah,
 * tombol hapus hanya untuk sumber kustom. Nama sumber yang dikosongkan otomatis
 * terdeteksi dari judul portal. Setiap perubahan langsung disimpan.
 */
public class CrawlSourcesDialog extends JDialog {

    private static final Color ACCENT_BLUE = new Color(0, 114, 188);
    private static final Color ROW_SEPARATOR = new Color(236, 238, 242);
    private static final int MAX_LIST_HEIGHT = 220;

    private final List<CrawlSource> sources;
    private final Path storeFile;
    private final Runnable onSourcesChanged;
    private JPanel rowsPanel;
    private JScrollPane listScroll;
    private JTextField urlField;
    private JTextField nameField;

    public CrawlSourcesDialog(Frame owner, List<CrawlSource> sources, Path storeFile, Runnable onSourcesChanged) {
        super(owner, "Kelola Sumber Crawl", true);
        this.sources = sources;
        this.storeFile = storeFile;
        this.onSourcesChanged = onSourcesChanged;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 18, 14, 18));

        // 1. Petunjuk singkat
        JLabel hint = new JLabel("Centang sumber yang dipakai saat crawl — pencarian memakai sumber aktif pertama.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(new Color(120, 120, 120));

        // 2. Baris tambah cepat: URL + Nama (opsional) + tombol — tanpa jendela kedua
        urlField = new JTextField("https://");
        urlField.putClientProperty("JTextField.placeholderText", "https://portal.ac.id");
        nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Nama (kosongkan = otomatis)");

        JButton addBtn = new JButton("＋ Tambah");
        addBtn.setBackground(ACCENT_BLUE);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusable(false);
        addBtn.addActionListener(e -> addSourceFromFields());

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 6);
        gc.weightx = 2;
        gc.gridx = 0;
        fields.add(urlField, gc);
        gc.weightx = 1;
        gc.gridx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        fields.add(nameField, gc);

        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(fields, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);

        // 3. Daftar sumber (checkbox aktif + hapus untuk kustom)
        rowsPanel = new JPanel();
        rowsPanel.setOpaque(true);
        rowsPanel.setBackground(Color.WHITE);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        listScroll = new JScrollPane(rowsPanel);
        listScroll.setBorder(new LineBorder(new Color(210, 214, 220), 1, true));
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(hint, BorderLayout.NORTH);
        top.add(addRow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton closeBtn = new JButton("Tutup");
        closeBtn.addActionListener(e -> dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(closeBtn);
        bottom.add(closeWrap, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(addBtn); // Enter di kolom URL/Nama langsung menambahkan

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
        // Tinggi daftar mengikuti jumlah baris (dibatasi) agar tidak ada kotak kosong
        int listHeight = Math.min(MAX_LIST_HEIGHT, rowsPanel.getPreferredSize().height + 6);
        listScroll.setPreferredSize(new Dimension(520, listHeight));
        listScroll.revalidate();
        if (isDisplayable()) {
            pack(); // dialog menyesuaikan ukurannya dengan isi
            setLocationRelativeTo(getOwner());
        }
    }

    private JPanel createSourceRow(CrawlSource source) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new CompoundBorder(
                new EmptyBorder(7, 4, 7, 4),
                new MatteBorder(0, 0, 1, 0, ROW_SEPARATOR)));

        JCheckBox checkbox = new JCheckBox(
                source.name() + " — " + source.baseUrl() + (source.builtin() ? "   [bawaan]" : ""),
                source.enabled());
        checkbox.setOpaque(false);
        checkbox.setToolTipText(source.builtin() ? "Sumber bawaan" : "Klik untuk aktif/nonaktif");
        checkbox.addActionListener(e -> updateSource(source, checkbox.isSelected()));
        row.add(checkbox, BorderLayout.CENTER);

        if (!source.builtin()) {
            JButton deleteBtn = new JButton("Hapus");
            deleteBtn.setFont(deleteBtn.getFont().deriveFont(11f));
            deleteBtn.setMargin(new Insets(2, 10, 2, 10));
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

    /** Tambah sumber langsung dari baris atas; nama kosong → otomatis dari host, lalu diperkaya. */
    private void addSourceFromFields() {
        String baseUrl = urlField.getText().strip();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            JOptionPane.showMessageDialog(this, "URL harus dimulai dengan http:// atau https://",
                    "Tambah Sumber", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        String typedName = nameField.getText().strip();
        boolean nameBlank = typedName.isEmpty() || typedName.contains("|");
        String name = nameBlank ? hostOf(baseUrl) : typedName;

        CrawlSource added = new CrawlSource(name, baseUrl, true, false);
        sources.add(added);
        persist();
        renderRows();
        urlField.setText("https://");
        nameField.setText("");

        if (nameBlank) {
            detectNameAsync(added); // perkaya nama dari judul portal di latar belakang
        }
    }

    /** Melengkapi nama sumber dari judul portal secara otomatis (latar belakang). */
    private void detectNameAsync(CrawlSource source) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    Document doc = Jsoup.connect(source.baseUrl() + "/documents?q=test")
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl Source Tester")
                            .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                            .timeout(15000)
                            .get();
                    String detected = doc.title();
                    return detected == null || detected.isBlank() ? null : detected.strip();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String detected = get();
                    if (detected == null) return;
                    int idx = sources.indexOf(source);
                    if (idx < 0) return;
                    String name = detected.length() > 40 ? detected.substring(0, 40).strip() : detected;
                    sources.set(idx, new CrawlSource(name, source.baseUrl(), source.enabled(), source.builtin()));
                    persist();
                    renderRows();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void updateSource(CrawlSource source, boolean enabled) {
        int idx = sources.indexOf(source);
        if (idx >= 0) {
            sources.set(idx, new CrawlSource(source.name(), source.baseUrl(), enabled, source.builtin()));
        }
        persist();
    }

    private String hostOf(String baseUrl) {
        try {
            String host = new URI(baseUrl).getHost();
            if (host == null) return baseUrl;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ex) {
            return baseUrl;
        }
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
