package id.gocrawl.gui;

import id.gocrawl.GarudaSubject;
import id.gocrawl.Journal;
import id.gocrawl.JournalCatalog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog visual untuk menambahkan jurnal target baru sebagai sumber crawling.
 * Dilengkapi pengujian koneksi langsung dan deteksi platform OJS.
 */
public class AddJournalDialog extends JDialog {

    private final JTextField urlField = new JTextField(30);
    private final JTextField nameField = new JTextField(30);
    private final JTextField publisherField = new JTextField(30);
    private final JComboBox<String> subjectCombo = new JComboBox<>();
    private final JComboBox<String> sintaCombo = new JComboBox<>(new String[]{"-", "S1", "S2", "S3", "S4", "S5", "S6"});
    private final JLabel statusLabel = new JLabel("Masukkan URL portal jurnal OJS di atas.");
    private final JButton testBtn = new JButton("Uji Koneksi & Deteksi");
    private final JButton saveBtn = new JButton("Simpan Jurnal");
    private final JButton cancelBtn = new JButton("Batal");

    private final Runnable onSuccessCallback;

    public AddJournalDialog(Frame owner, Runnable onSuccessCallback) {
        super(owner, "Tambah Jurnal Sumber Baru", true);
        this.onSuccessCallback = onSuccessCallback;
        initUI();
    }

    public AddJournalDialog(Dialog owner, Runnable onSuccessCallback) {
        super(owner, "Tambah Jurnal Sumber Baru", true);
        this.onSuccessCallback = onSuccessCallback;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(520, 360);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        // Isi ComboBox Subjek
        subjectCombo.addItem("- Pilih Subjek Garuda -");
        for (var entry : GarudaSubject.OFFICIAL_SUBJECTS.entrySet()) {
            subjectCombo.addItem(entry.getKey() + " - " + entry.getValue());
        }

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Baris 1: URL Jurnal + Tombol Test
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("URL Jurnal OJS:"), gbc);

        JPanel urlBox = new JPanel(new BorderLayout(5, 0));
        urlBox.add(urlField, BorderLayout.CENTER);
        urlBox.add(testBtn, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        formPanel.add(urlBox, gbc);

        // Baris 2: Status Koneksi
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Status:"), gbc);
        statusLabel.setForeground(Color.GRAY);
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(statusLabel, gbc);

        // Baris 3: Nama Jurnal
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Nama Jurnal:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        formPanel.add(nameField, gbc);

        // Baris 4: Penerbit / Institusi
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Penerbit:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        formPanel.add(publisherField, gbc);

        // Baris 5: Subjek Garuda
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Subjek Garuda:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        formPanel.add(subjectCombo, gbc);

        // Baris 6: Peringkat SINTA
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Akreditasi SINTA:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        formPanel.add(sintaCombo, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Tombol Aksi Bawah
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Event Handlers
        testBtn.addActionListener(e -> testConnection());
        saveBtn.addActionListener(e -> saveJournal());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void testConnection() {
        String rawUrl = urlField.getText().strip();
        if (rawUrl.isEmpty()) {
            statusLabel.setText("Ketikkan URL jurnal terlebih dahulu.");
            statusLabel.setForeground(Color.RED);
            return;
        }
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://" + rawUrl;
            urlField.setText(rawUrl);
        }

        testBtn.setEnabled(false);
        statusLabel.setText("Menguji koneksi ke server jurnal...");
        statusLabel.setForeground(Color.BLUE);

        final String targetUrl = rawUrl;
        SwingWorker<Document, Void> worker = new SwingWorker<>() {
            @Override
            protected Document doInBackground() throws Exception {
                return Jsoup.connect(targetUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl Probe")
                        .timeout(12000)
                        .followRedirects(true)
                        .get();
            }

            @Override
            protected void done() {
                testBtn.setEnabled(true);
                try {
                    Document doc = get();
                    String pageTitle = doc.title().strip();
                    boolean isOjs = doc.html().contains("Open Journal Systems")
                            || doc.html().contains("/article/view/")
                            || doc.select("meta[name=generator][content*=Open Journal Systems]").size() > 0;

                    if (isOjs) {
                        statusLabel.setText("Terhubung! Terdeteksi sistem Open Journal Systems.");
                        statusLabel.setForeground(new Color(34, 139, 34)); // Forest Green
                    } else {
                        statusLabel.setText("Terhubung (Platform web umum / OJS non-standar).");
                        statusLabel.setForeground(new Color(180, 120, 0));
                    }

                    if (nameField.getText().isBlank() && !pageTitle.isEmpty()) {
                        nameField.setText(pageTitle);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Gagal terhubung: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    private void saveJournal() {
        String url = urlField.getText().strip();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "URL Jurnal wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = nameField.getText().strip();
        if (name.isEmpty()) {
            name = URI.create(url).getHost();
        }
        String publisher = publisherField.getText().strip();

        String selectedSubj = (String) subjectCombo.getSelectedItem();
        String subjectId = "";
        String subjectName = "";
        if (selectedSubj != null && selectedSubj.contains(" - ")) {
            subjectId = selectedSubj.split(" - ")[0].strip();
            subjectName = selectedSubj.split(" - ")[1].strip();
        }

        String sinta = (String) sintaCombo.getSelectedItem();
        if (sinta == null) sinta = "-";

        try {
            // 1. Simpan URL ke journal.txt
            Path journalTxt = Path.of("journal.txt");
            List<String> lines = Files.exists(journalTxt) ? Files.readAllLines(journalTxt, StandardCharsets.UTF_8) : new ArrayList<>();
            if (!lines.contains(url)) {
                Files.writeString(journalTxt, (lines.isEmpty() ? "" : "\n") + url,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            // 2. Simpan entitas ke data/journals_master.json
            Path masterFile = Path.of("data", "journals_master.json");
            JournalCatalog catalog = JournalCatalog.load(masterFile);
            String syntheticId = "custom-" + Math.abs(url.hashCode());
            Journal j = new Journal(
                    syntheticId,
                    name,
                    publisher.isEmpty() ? "-" : publisher,
                    syntheticId,
                    subjectId,
                    subjectName,
                    sinta,
                    url,
                    url.replaceAll("/+$", "") + "/oai",
                    "-",
                    "-",
                    true
            );
            catalog.register(j);
            JournalCatalog.save(masterFile, catalog.allJournals());

            JOptionPane.showMessageDialog(this,
                    "Jurnal berhasil ditambahkan ke daftar target dan katalog!",
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal menyimpan jurnal: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
