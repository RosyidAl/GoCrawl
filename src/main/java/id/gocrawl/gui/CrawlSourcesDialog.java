package id.gocrawl.gui;

import id.gocrawl.CrawlSource;
import id.gocrawl.CrawlSourceStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
import java.util.function.Consumer;

/**
 * Dialog "Kelola Sumber Crawl": satu jendela untuk semuanya — baris tambah cepat
 * (URL + Nama opsional + Uji Koneksi) di atas, daftar sumber dengan checkbox aktif
 * di bawah, tombol hapus hanya untuk sumber kustom, dan tombol Simpan eksplisit.
 * Nama & tipe sumber otomatis terdeteksi dari portal. Setiap perubahan langsung
 * disimpan ke data/crawl_sources.txt.
 */
public class CrawlSourcesDialog extends JDialog {

    private static final Color ACCENT_BLUE = new Color(0, 114, 188);
    private static final Color ROW_SEPARATOR = new Color(236, 238, 242);
    private static final Color STATUS_INFO = new Color(110, 110, 110);
    private static final Color STATUS_OK = new Color(34, 139, 34);
    private static final Color STATUS_ERROR = new Color(198, 40, 40);
    private static final int MAX_LIST_HEIGHT = 220;

    private final List<CrawlSource> sources;
    private final Path storeFile;
    private final Runnable onSourcesChanged;
    private JPanel rowsPanel;
    private JScrollPane listScroll;
    private JTextField urlField;
    private JTextField nameField;
    private JButton testBtn;
    private JLabel addStatusLabel;

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

        // 2. Baris tambah cepat: URL + Uji Koneksi (validasi), lalu Nama + Tambah
        urlField = new JTextField();
        urlField.putClientProperty("JTextField.placeholderText", "https://portal.ac.id");
        nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Nama sumber (kosongkan = otomatis)");

        testBtn = new JButton("Uji Koneksi");
        testBtn.setFont(testBtn.getFont().deriveFont(11f));
        testBtn.setMargin(new Insets(2, 10, 2, 10));
        testBtn.addActionListener(e -> testConnection());

        JButton addBtn = new JButton("＋ Tambah");
        addBtn.setBackground(ACCENT_BLUE);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusable(false);
        addBtn.addActionListener(e -> addSourceFromFields());

        JPanel addRows = new JPanel(new GridLayout(2, 1, 0, 6));
        addRows.setOpaque(false);

        JPanel urlRow = new JPanel(new BorderLayout(6, 0));
        urlRow.setOpaque(false);
        urlRow.add(urlField, BorderLayout.CENTER);
        urlRow.add(testBtn, BorderLayout.EAST);
        addRows.add(urlRow);

        JPanel nameRow = new JPanel(new BorderLayout(6, 0));
        nameRow.setOpaque(false);
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.add(addBtn, BorderLayout.EAST);
        addRows.add(nameRow);

        addStatusLabel = new JLabel(" ");
        addStatusLabel.setFont(addStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel addSection = new JPanel(new BorderLayout(0, 4));
        addSection.setOpaque(false);
        addSection.add(addRows, BorderLayout.CENTER);
        addSection.add(addStatusLabel, BorderLayout.SOUTH);

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
        top.add(addSection, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JButton saveBtn = new JButton("Simpan");
        saveBtn.addActionListener(e -> {
            persist();
            JOptionPane.showMessageDialog(this, "Daftar sumber tersimpan.",
                    "Kelola Sumber Crawl", JOptionPane.INFORMATION_MESSAGE);
        });
        JButton closeBtn = new JButton("Tutup");
        closeBtn.addActionListener(e -> dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(saveBtn);
        closeWrap.add(Box.createHorizontalStrut(8));
        closeWrap.add(closeBtn);
        bottom.add(closeWrap, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        renderRows();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Menyusun ulang daftar baris sumber (checkbox + URL muted + hapus untuk kustom). */
    private void renderRows() {
        rowsPanel.removeAll();
        for (CrawlSource source : sources) {
            rowsPanel.add(createSourceRow(source));
        }
        rowsPanel.revalidate();
        rowsPanel.repaint();
        // Tinggi daftar mengikuti jumlah baris (dibatasi) agar tidak ada kotak kosong
        int listHeight = Math.min(MAX_LIST_HEIGHT, rowsPanel.getPreferredSize().height + 6);
        listScroll.setPreferredSize(new Dimension(560, listHeight));
        listScroll.revalidate();
        if (isDisplayable()) {
            pack(); // dialog menyesuaikan ukurannya dengan isi
            setLocationRelativeTo(getOwner());
        }
    }

    /** Baris sumber dua tingkat: nama (checkbox) di atas, URL redup di bawah. */
    private JPanel createSourceRow(CrawlSource source) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new CompoundBorder(
                new EmptyBorder(6, 4, 6, 4),
                new MatteBorder(0, 0, 1, 0, ROW_SEPARATOR)));

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        JCheckBox checkbox = new JCheckBox(source.name() + (source.builtin() ? "   [bawaan]" : ""), source.enabled());
        checkbox.setOpaque(false);
        checkbox.setToolTipText(source.builtin() ? "Sumber bawaan" : "Klik untuk aktif/nonaktif");
        checkbox.addActionListener(e -> updateSource(source, checkbox.isSelected()));
        text.add(checkbox, BorderLayout.NORTH);

        JLabel urlLabel = new JLabel(source.baseUrl());
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.PLAIN, 11f));
        urlLabel.setForeground(new Color(110, 110, 110));
        urlLabel.setBorder(new EmptyBorder(0, 26, 0, 0));
        text.add(urlLabel, BorderLayout.SOUTH);
        row.add(text, BorderLayout.CENTER);

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
            setStatus("URL harus dimulai dengan http:// atau https://", STATUS_ERROR);
            return;
        }
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        String typedName = nameField.getText().strip();
        boolean nameBlank = typedName.isEmpty() || typedName.contains("|");
        String name = nameBlank ? hostOf(baseUrl) : typedName;
        // Tebakan awal tipe: URL dengan path /index.php/ hampir pasti OJS
        String guessedType = baseUrl.contains("/index.php/")
                ? CrawlSource.TYPE_OJS : CrawlSource.TYPE_GARUDA;

        CrawlSource added = new CrawlSource(name, baseUrl, guessedType, true, false);
        sources.add(added);
        persist();
        renderRows();
        urlField.setText("");
        nameField.setText("");

        // Nama & tipe diperkaya otomatis dari judul/meta portal di latar belakang
        if (nameBlank) {
            setStatus("Mengambil nama portal ...", STATUS_INFO);
        } else {
            setStatus("Sumber ditambahkan — mendeteksi tipe portal ...", STATUS_INFO);
        }
        detectPortalInfoAsync(added, nameBlank);
    }

    /** Uji koneksi ke URL pada kolom sumber: validasi + deteksi judul portal. */
    private void testConnection() {
        String baseUrl = urlField.getText().strip();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            setStatus("URL harus dimulai dengan http:// atau https://", STATUS_ERROR);
            return;
        }
        final String cleanUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        testBtn.setEnabled(false);
        setStatus("Menguji koneksi ke " + cleanUrl + " ...", STATUS_INFO);
        probePortalAsync(cleanUrl,
                info -> {
                    testBtn.setEnabled(true);
                    if (info == null) {
                        setStatus("Gagal terhubung (portal offline / menolak akses).", STATUS_ERROR);
                    } else {
                        setStatus("Terhubung — portal: " + info[0] + "  (" + info[1] + ")", STATUS_OK);
                        nameField.setText(shortPortalName(info[0]));
                    }
                },
                ex -> {
                    testBtn.setEnabled(true);
                    setStatus("Gagal terhubung: " + ex.getMessage(), STATUS_ERROR);
                });
    }

    /**
     * Mengambil info portal untuk validasi koneksi: [judul, tipe]. Berlapis: coba
     * dulu path pencarian gaya Garuda (/documents?q=...), bila tidak ada (portal
     * OJS lain) jatuh ke URL dasarnya. Tipe dideteksi dari meta generator. Satu
     * kali percobaan ulang setelah jeda 3 detik. Null berarti gagal.
     */
    private String[] fetchPortalInfo(String baseUrl) {
        String[] candidates = { baseUrl + "/documents?q=test", baseUrl };
        for (String url : candidates) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    org.jsoup.Connection.Response response = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl Source Tester")
                            .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                            .timeout(15000)
                            .ignoreHttpErrors(true)
                            .execute();
                    if (response.statusCode() == 200) {
                        Document doc = response.parse();
                        String title = doc.title();
                        Element generatorEl = doc.selectFirst("meta[name=generator]");
                        String generator = generatorEl == null ? "" : generatorEl.attr("content").toLowerCase();
                        String type = generator.contains("open journal systems")
                                ? CrawlSource.TYPE_OJS : CrawlSource.TYPE_GARUDA;
                        return new String[]{
                                title == null || title.isBlank() ? "Sumber kustom" : title.strip(),
                                type};
                    }
                } catch (Exception ignored) {
                    // coba lagi pada percobaan berikutnya
                }
                if (attempt == 1) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /** Melengkapi nama dan tipe sumber dari judul portal secara otomatis (latar belakang). */
    private void detectPortalInfoAsync(CrawlSource source, boolean updateName) {
        probePortalAsync(source.baseUrl(), info -> {
            int idx = sources.indexOf(source);
            if (idx < 0) return;
            if (info == null) {
                setStatus("Deteksi portal gagal — host URL & tipe Garuda dipakai.", STATUS_ERROR);
                return;
            }
            String name = updateName ? shortPortalName(info[0]) : source.name();
            sources.set(idx, new CrawlSource(name, source.baseUrl(), info[1], source.enabled(), source.builtin()));
            persist();
            renderRows();
            setStatus("Portal terdeteksi: " + info[0] + "  (" + info[1] + ")", STATUS_OK);
        }, ignored -> setStatus("Deteksi portal gagal — host URL & tipe Garuda dipakai.", STATUS_ERROR));
    }

    private void probePortalAsync(String baseUrl, Consumer<String[]> onSuccess, Consumer<Exception> onFailure) {
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                return fetchPortalInfo(baseUrl);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    onFailure.accept(ex);
                }
            }
        }.execute();
    }

    private static String shortPortalName(String name) {
        return name.length() > 40 ? name.substring(0, 40).strip() : name;
    }

    private void updateSource(CrawlSource source, boolean enabled) {
        int idx = sources.indexOf(source);
        if (idx >= 0) {
            sources.set(idx, new CrawlSource(source.name(), source.baseUrl(), source.type(), enabled, source.builtin()));
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

    private void setStatus(String text, Color color) {
        addStatusLabel.setForeground(color);
        addStatusLabel.setText(text);
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
