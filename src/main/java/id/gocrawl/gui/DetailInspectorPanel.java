package id.gocrawl.gui;

import id.gocrawl.Article;
import id.gocrawl.JournalCatalog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Panel Kanan (Inspector Detail ala Mendeley): Menampilkan detail metadata lengkap artikel,
 * termasuk asal institusi/penerbit, jurnal, penulis, DOI ber-hyperlink, teks abstrak lengkap,
 * serta tombol aksi web & pengunduh PDF.
 *
 * Menggunakan ResponsiveScrollPanel (Scrollable) untuk mencegah bug teks melar horizontal
 * dan mengeliminasi nested scrollbar.
 */
public class DetailInspectorPanel extends JPanel {

    private final JTextArea titleArea = new JTextArea();
    private final JLabel originLabel = new JLabel("-");
    private final JLabel journalLabel = new JLabel("-");
    private final JLabel coreSubjectLabel = new JLabel("-");
    private final JLabel subjectLabel = new JLabel("-");
    private final JLabel sintaLabel = new JLabel("-");
    private final JTextArea authorsArea = new JTextArea("-");
    private final JLabel yearLabel = new JLabel("-");
    private final JLabel doiLabel = new JLabel("-");
    private final JLabel urlLabel = new JLabel("-");
    private final JTextArea abstractArea = new JTextArea();

    private final JButton openWebBtn = new JButton("Buka di Web (OJS)");
    private final JButton downloadPdfBtn = new JButton("Unduh PDF");
    private final JLabel downloadStatusLabel = new JLabel("");

    private final JournalCatalog catalog;
    private Article currentArticle;

    public DetailInspectorPanel(JournalCatalog catalog) {
        this.catalog = catalog;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setMinimumSize(new Dimension(320, 400));
        setPreferredSize(new Dimension(380, 650));

        // 1. Header Atas
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 4, 8, 4));
        JLabel headerLabel = new JLabel("RINCIAN METADATA ARTIKEL");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));
        headerLabel.setForeground(new Color(110, 110, 110));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Single Responsive Scrollable Content Container
        ResponsiveScrollPanel contentPanel = new ResponsiveScrollPanel();

        // Kartu Judul
        JPanel titleCard = new JPanel(new BorderLayout());
        titleCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        titleArea.setEditable(false);
        titleArea.setLineWrap(true);
        titleArea.setWrapStyleWord(true);
        titleArea.setFont(titleArea.getFont().deriveFont(Font.BOLD, 14f));
        titleArea.setText("Pilih sebuah artikel dari tabel untuk melihat rincian metadata.");
        titleCard.add(titleArea, BorderLayout.CENTER);
        contentPanel.add(titleCard);
        contentPanel.add(Box.createVerticalStrut(12));

        // Form Metadata Terstruktur
        JPanel metaBox = new JPanel(new GridBagLayout());
        metaBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 2, 3, 2);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Asal / Penerbit
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Asal / Penerbit:"), gbc);
        originLabel.setFont(originLabel.getFont().deriveFont(Font.BOLD));
        originLabel.setForeground(new Color(30, 80, 180));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(originLabel, gbc);

        // Nama Jurnal
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Nama Jurnal:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(journalLabel, gbc);

        // Core Subject
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Core Subject:"), gbc);
        coreSubjectLabel.setForeground(new Color(30, 80, 160));
        coreSubjectLabel.setFont(coreSubjectLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(coreSubjectLabel, gbc);

        // Bidang / Subjek
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Bidang / Subjek:"), gbc);
        subjectLabel.setForeground(new Color(60, 120, 60));
        subjectLabel.setFont(subjectLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(subjectLabel, gbc);

        // Akreditasi SINTA
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Akreditasi SINTA:"), gbc);
        sintaLabel.setForeground(new Color(180, 80, 20));
        sintaLabel.setFont(sintaLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(sintaLabel, gbc);

        // Penulis
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Penulis:"), gbc);
        authorsArea.setEditable(false);
        authorsArea.setLineWrap(true);
        authorsArea.setWrapStyleWord(true);
        authorsArea.setBackground(metaBox.getBackground());
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(authorsArea, gbc);

        // Tahun
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Tahun:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(yearLabel, gbc);

        // DOI
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("DOI:"), gbc);
        doiLabel.setForeground(new Color(0, 102, 204));
        doiLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        doiLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentArticle != null && currentArticle.doi() != null && !currentArticle.doi().isBlank()) {
                    openBrowser("https://doi.org/" + currentArticle.doi());
                }
            }
        });
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(doiLabel, gbc);

        // Tautan Web
        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.3; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Tautan OJS:"), gbc);
        urlLabel.setForeground(new Color(0, 102, 204));
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentArticle != null && currentArticle.url() != null && !currentArticle.url().isBlank()) {
                    openBrowser(currentArticle.url());
                }
            }
        });
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(urlLabel, gbc);

        contentPanel.add(metaBox);
        contentPanel.add(Box.createVerticalStrut(12));

        // Bagian Abstrak
        JPanel abstractCard = new JPanel(new BorderLayout(0, 6));
        abstractCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel absTitle = new JLabel("ABSTRAK LENGKAP");
        absTitle.setFont(absTitle.getFont().deriveFont(Font.BOLD, 11f));
        absTitle.setForeground(new Color(110, 110, 110));
        abstractCard.add(absTitle, BorderLayout.NORTH);

        abstractArea.setEditable(false);
        abstractArea.setLineWrap(true);
        abstractArea.setWrapStyleWord(true);
        abstractArea.setFont(abstractArea.getFont().deriveFont(12.5f));
        abstractCard.add(abstractArea, BorderLayout.CENTER);

        contentPanel.add(abstractCard);

        // Single ScrollPane pembungkus
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bilah Tombol Aksi Bawah
        JPanel bottomActionPanel = new JPanel(new BorderLayout(0, 4));
        bottomActionPanel.setBorder(new EmptyBorder(10, 2, 2, 2));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.add(openWebBtn);
        btnRow.add(downloadPdfBtn);
        bottomActionPanel.add(btnRow, BorderLayout.NORTH);

        downloadStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        downloadStatusLabel.setFont(downloadStatusLabel.getFont().deriveFont(11f));
        bottomActionPanel.add(downloadStatusLabel, BorderLayout.SOUTH);

        add(bottomActionPanel, BorderLayout.SOUTH);

        // Setup awal: Kosong
        setArticle(null);

        // Events
        openWebBtn.addActionListener(e -> {
            if (currentArticle != null && currentArticle.url() != null && !currentArticle.url().isBlank()) {
                openBrowser(currentArticle.url());
            }
        });
        downloadPdfBtn.addActionListener(e -> handleDownloadPdf());
    }

    public void setArticle(Article article) {
        this.currentArticle = article;
        if (article == null) {
            titleArea.setText("Pilih salah satu artikel di tabel untuk membaca rincian lengkap.");
            originLabel.setText("-");
            journalLabel.setText("-");
            coreSubjectLabel.setText("-");
            subjectLabel.setText("-");
            sintaLabel.setText("-");
            authorsArea.setText("-");
            yearLabel.setText("-");
            doiLabel.setText("-");
            urlLabel.setText("-");
            abstractArea.setText("");
            openWebBtn.setEnabled(false);
            downloadPdfBtn.setEnabled(false);
            downloadStatusLabel.setText("");
            return;
        }

        titleArea.setText(article.title());

        String origin = "-";
        if (catalog != null) {
            origin = catalog.resolveOrigin(article.url(), article.journal());
        }
        originLabel.setText(origin);
        journalLabel.setText(article.journal() == null || article.journal().isBlank() ? "-" : article.journal());

        String coreSubject = "-";
        if (catalog != null) {
            coreSubject = catalog.resolveCoreSubject(article.url(), article.journal());
        }
        coreSubjectLabel.setText(coreSubject);

        String subject = "-";
        if (catalog != null) {
            id.gocrawl.Journal j = catalog.findByUrlOrHost(article.url());
            if (j == null) {
                j = catalog.findByName(article.journal());
            }
            if (j != null) {
                String subId = j.subjectId();
                if ((subId == null || subId.isBlank() || subId.equals("-")) && j.subjectName() != null && !j.subjectName().isBlank()) {
                    subId = id.gocrawl.GarudaSubject.resolveSubjectId(j.subjectName());
                }
                if (subId != null && !subId.isBlank() && !subId.equals("-")) {
                    String label = id.gocrawl.GarudaSubject.getDisplayLabel(subId);
                    if (label != null && !label.equals("Unknown Subject")) {
                        subject = label;
                    }
                }
                if (subject.equals("-") && j.subjectName() != null && !j.subjectName().isBlank() && !j.subjectName().equals("-")) {
                    subject = j.subjectName();
                }
            }
        }
        subjectLabel.setText(subject);

        String sinta = "-";
        if (catalog != null) {
            sinta = catalog.resolveSintaRank(article.url(), article.journal());
        }
        if (sinta.equals("-") || sinta.isBlank()) {
            sintaLabel.setText("Belum Terakreditasi / Non-SINTA");
            sintaLabel.setForeground(Color.GRAY);
        } else {
            sintaLabel.setText("SINTA " + sinta.replace("S", "") + " (" + sinta + ")");
            sintaLabel.setForeground(new Color(180, 80, 20));
        }

        authorsArea.setText(article.authors() == null || article.authors().isBlank() ? "-" : article.authors().replace(";", "\n• "));
        yearLabel.setText(article.year() == null || article.year().isBlank() ? "-" : article.year());
        doiLabel.setText(article.doi() == null || article.doi().isBlank() ? "Tidak tersedia" : article.doi());

        if (article.url() != null && !article.url().isBlank()) {
            String shortUrl = article.url();
            if (shortUrl.length() > 38) shortUrl = shortUrl.substring(0, 35) + "...";
            urlLabel.setText(shortUrl);
            urlLabel.setToolTipText(article.url());
        } else {
            urlLabel.setText("-");
            urlLabel.setToolTipText(null);
        }

        abstractArea.setText(article.abstractText() == null || article.abstractText().isBlank()
                ? "(Abstrak artikel tidak disediakan pada metadata portal jurnal)"
                : article.abstractText());
        abstractArea.setCaretPosition(0);

        openWebBtn.setEnabled(article.url() != null && !article.url().isBlank());
        downloadPdfBtn.setEnabled(article.url() != null && !article.url().isBlank());
        downloadStatusLabel.setText("");
    }

    private void handleDownloadPdf() {
        if (currentArticle == null || currentArticle.url() == null || currentArticle.url().isBlank()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Berkas PDF Artikel");
        String safeTitle = currentArticle.title().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeTitle.length() > 60) safeTitle = safeTitle.substring(0, 60);
        chooser.setSelectedFile(new File(safeTitle + ".pdf"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = chooser.getSelectedFile();
        downloadPdfBtn.setEnabled(false);
        downloadStatusLabel.setText("Mencari berkas PDF di portal OJS...");
        downloadStatusLabel.setForeground(Color.BLUE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String resolvedUrl = "";

            @Override
            protected Boolean doInBackground() throws Exception {
                Document doc = Jsoup.connect(currentArticle.url())
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl PDF Downloader")
                        .timeout(15000)
                        .followRedirects(true)
                        .get();

                Element metaPdf = doc.selectFirst("meta[name=citation_pdf_url]");
                if (metaPdf != null && !metaPdf.attr("content").isBlank()) {
                    resolvedUrl = metaPdf.attr("content").strip();
                }

                if (resolvedUrl.isEmpty()) {
                    Element aPdf = doc.selectFirst("a.obj_galley_link.pdf, a.pdf, a[href*='/article/download/'], a[href*='/article/view/'][href$='.pdf']");
                    if (aPdf != null) {
                        resolvedUrl = aPdf.absUrl("href");
                    }
                }

                if (resolvedUrl.contains("/article/view/")) {
                    resolvedUrl = resolvedUrl.replace("/article/view/", "/article/download/");
                }

                if (resolvedUrl.isEmpty()) {
                    throw new IllegalStateException("Link PDF tidak ditemukan di halaman jurnal.");
                }

                try (InputStream in = URI.create(resolvedUrl).toURL().openStream()) {
                    Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            }

            @Override
            protected void done() {
                downloadPdfBtn.setEnabled(true);
                try {
                    get();
                    downloadStatusLabel.setText("PDF berhasil diunduh ke: " + targetFile.getName());
                    downloadStatusLabel.setForeground(new Color(34, 139, 34));
                } catch (Exception ex) {
                    downloadStatusLabel.setText("Gagal mengunduh PDF: " + ex.getMessage());
                    downloadStatusLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal membuka browser: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(new Color(110, 110, 110));
        return label;
    }

    /**
     * Panel khusus yang mengimplementasikan Scrollable agar lebar konten selalu
     * mengikuti lebar viewport secara responsif (tidak melar melebihi batas).
     */
    private static class ResponsiveScrollPanel extends JPanel implements Scrollable {
        public ResponsiveScrollPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(4, 4, 10, 4));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // KUNCI: Mencegah bug teks melebar secara horizontal
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
