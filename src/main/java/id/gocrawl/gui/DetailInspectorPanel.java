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
import java.util.ArrayList;
import java.util.List;

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
    private Runnable onClose;

    // Kolom label metadata: dikunci ke lebar label terlebar agar tidak terpotong saat panel sempit
    private final List<JLabel> metaFieldLabels = new ArrayList<>();
    private int metaLabelColumnWidth = 0;

    public DetailInspectorPanel(JournalCatalog catalog) {
        this.catalog = catalog;
        initUI();
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(AcademicTheme.SURFACE);
        // Padding kiri tipis agar konten tidak terkesan terdesak ke kanan
        setBorder(new EmptyBorder(4, 4, 10, 14));
        setMinimumSize(new Dimension(320, 400));
        setPreferredSize(new Dimension(380, 650));

        // Konten scrollable responsif
        ResponsiveScrollPanel contentPanel = new ResponsiveScrollPanel();
        contentPanel.setBackground(AcademicTheme.SURFACE);

        // Kartu Judul + tombol tutup (×)
        JPanel titleCard = new JPanel(new BorderLayout(8, 0));
        titleCard.setBackground(AcademicTheme.CARD_BG);
        titleCard.setBorder(AcademicTheme.cardBorder(12));

        JButton closeBtn = new JButton(AcademicIcons.close(AcademicTheme.TEXT_MUTED, 14));
        closeBtn.setToolTipText("Tutup Inspector");
        closeBtn.setFocusPainted(false);
        closeBtn.setFocusable(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            if (onClose != null) onClose.run();
        });
        JPanel closeWrap = new JPanel(new BorderLayout());
        closeWrap.setOpaque(false);
        closeWrap.add(closeBtn, BorderLayout.NORTH);
        titleCard.add(closeWrap, BorderLayout.EAST);

        titleArea.setEditable(false);
        titleArea.setLineWrap(true);
        titleArea.setWrapStyleWord(true);
        titleArea.setBackground(AcademicTheme.CARD_BG);
        titleArea.setForeground(AcademicTheme.ON_SURFACE);
        titleArea.setFont(AcademicTheme.uiFont(Font.BOLD, 14.5f));
        titleArea.setText("Pilih sebuah artikel dari tabel untuk melihat rincian metadata.");
        titleCard.add(titleArea, BorderLayout.CENTER);
        contentPanel.add(titleCard);
        contentPanel.add(Box.createVerticalStrut(10));

        // Form Metadata Terstruktur
        JPanel metaBox = new JPanel(new GridBagLayout());
        metaBox.setBackground(AcademicTheme.CARD_BG);
        metaBox.setBorder(AcademicTheme.cardBorder(12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 2, 3, 2);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Asal / Penerbit
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Penerbit:"), gbc);
        originLabel.setFont(AcademicTheme.uiFont(Font.BOLD, 12.5f));
        originLabel.setForeground(AcademicTheme.PRIMARY_DARK);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(originLabel, gbc);

        // Nama Jurnal
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Nama Jurnal:"), gbc);
        journalLabel.setFont(AcademicTheme.uiFont(Font.PLAIN, 12.5f));
        journalLabel.setForeground(AcademicTheme.ON_SURFACE);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(journalLabel, gbc);

        // Core Subject
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Core Subject:"), gbc);
        coreSubjectLabel.setForeground(AcademicTheme.ON_SURFACE);
        coreSubjectLabel.setFont(AcademicTheme.uiFont(Font.BOLD, 12.5f));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(coreSubjectLabel, gbc);

        // Bidang / Subjek
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Bidang:"), gbc);
        subjectLabel.setForeground(AcademicTheme.SECONDARY);
        subjectLabel.setFont(AcademicTheme.uiFont(Font.BOLD, 12.5f));
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(subjectLabel, gbc);

        // Akreditasi
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Akreditasi:"), gbc);
        sintaLabel.setForeground(new Color(180, 80, 20));
        sintaLabel.setFont(sintaLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(sintaLabel, gbc);

        // Penulis
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Penulis:"), gbc);
        authorsArea.setEditable(false);
        authorsArea.setLineWrap(true);
        authorsArea.setWrapStyleWord(true);
        authorsArea.setBackground(metaBox.getBackground());
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(authorsArea, gbc);

        // Tahun
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Tahun:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(yearLabel, gbc);

        // DOI
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("DOI:"), gbc);
        doiLabel.setForeground(new Color(0, 102, 204));
        doiLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        doiLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentArticle != null && currentArticle.doi() != null && !currentArticle.doi().isBlank() && !currentArticle.doi().equals("-")) {
                    openBrowser("https://doi.org/" + currentArticle.doi());
                }
            }
        });
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(doiLabel, gbc);

        // Tautan Web
        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        metaBox.add(createFieldLabel("Tautan OJS:"), gbc);
        urlLabel.setForeground(new Color(0, 102, 204));
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentArticle != null && currentArticle.url() != null && !currentArticle.url().isBlank() && !currentArticle.url().equals("-")) {
                    openBrowser(currentArticle.url());
                }
            }
        });
        gbc.gridx = 1; gbc.gridy = 8; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        metaBox.add(urlLabel, gbc);

        contentPanel.add(metaBox);
        contentPanel.add(Box.createVerticalStrut(12));

        // Kunci lebar minimum seluruh label ke label terlebar ("Nama Jurnal:") agar
        // tidak terpotong ("N...") ketika panel kanan dalam kondisi sempit
        for (JLabel label : metaFieldLabels) {
            label.setMinimumSize(new Dimension(metaLabelColumnWidth + 4, label.getPreferredSize().height));
        }

        // Bagian Abstrak
        JPanel abstractCard = new JPanel(new BorderLayout(0, 6));
        abstractCard.setBackground(AcademicTheme.CARD_BG);
        abstractCard.setBorder(AcademicTheme.cardBorder(12));
        JLabel absTitle = new JLabel("Abstrak");
        absTitle.setFont(AcademicTheme.uiFont(Font.BOLD, 12f));
        absTitle.setForeground(AcademicTheme.ON_SURFACE);
        abstractCard.add(absTitle, BorderLayout.NORTH);

        abstractArea.setEditable(false);
        abstractArea.setLineWrap(true);
        abstractArea.setWrapStyleWord(true);
        abstractArea.setBackground(AcademicTheme.CARD_BG);
        abstractArea.setForeground(AcademicTheme.ON_SURFACE_VARIANT);
        abstractArea.setFont(AcademicTheme.uiFont(Font.PLAIN, 13f));
        abstractCard.add(abstractArea, BorderLayout.CENTER);

        contentPanel.add(abstractCard);

        // Single ScrollPane pembungkus
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(AcademicTheme.SURFACE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bilah Tombol Aksi Bawah
        JPanel bottomActionPanel = new JPanel(new BorderLayout(0, 4));
        bottomActionPanel.setOpaque(false);
        bottomActionPanel.setBorder(new EmptyBorder(10, 2, 2, 2));

        AcademicTheme.styleSecondaryButton(openWebBtn);
        AcademicTheme.stylePrimaryButton(downloadPdfBtn);
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setOpaque(false);
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
            sintaLabel.setOpaque(false);
            sintaLabel.setBorder(null);
            sintaLabel.setForeground(AcademicTheme.TEXT_MUTED);
            authorsArea.setText("-");
            yearLabel.setText("-");
            doiLabel.setText("-");
            doiLabel.setForeground(Color.GRAY);
            doiLabel.setCursor(Cursor.getDefaultCursor());
            doiLabel.setToolTipText(null);

            urlLabel.setText("-");
            urlLabel.setForeground(Color.GRAY);
            urlLabel.setCursor(Cursor.getDefaultCursor());
            urlLabel.setToolTipText(null);
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
            sintaLabel.setText("Belum Terakreditasi");
            sintaLabel.setOpaque(false);
            sintaLabel.setBorder(null);
            sintaLabel.setForeground(AcademicTheme.TEXT_MUTED);
        } else {
            String rankUpper = sinta.toUpperCase();
            Color[] colors = AcademicTheme.sintaBadgeColors(rankUpper);
            sintaLabel.setText(rankUpper);
            sintaLabel.setOpaque(false);
            sintaLabel.setBorder(null);
            sintaLabel.setForeground(colors[0]);
        }

        authorsArea.setText(article.authors() == null || article.authors().isBlank() ? "-" : article.authors().replace(";", "\n• "));
        yearLabel.setText(article.year() == null || article.year().isBlank() ? "-" : article.year());
        String doi = article.doi();
        if (doi != null && !doi.isBlank() && !doi.equals("-")) {
            String displayDoi = doi.strip();
            if (displayDoi.length() > 38) {
                displayDoi = displayDoi.substring(0, 35) + "...";
            }
            doiLabel.setFont(AcademicTheme.monoFont(Font.PLAIN, 12f));
            doiLabel.setText("<html><a href='#' style='color:#2563EB; text-decoration:underline;'>" + escapeHtml(displayDoi) + "</a></html>");
            doiLabel.setForeground(AcademicTheme.PRIMARY);
            doiLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            doiLabel.setToolTipText("Buka tautan DOI: https://doi.org/" + doi.strip());
        } else {
            doiLabel.setFont(AcademicTheme.uiFont(Font.PLAIN, 12f));
            doiLabel.setText("Tidak tersedia");
            doiLabel.setForeground(AcademicTheme.TEXT_MUTED);
            doiLabel.setCursor(Cursor.getDefaultCursor());
            doiLabel.setToolTipText(null);
        }

        String url = article.url();
        if (url != null && !url.isBlank() && !url.equals("-")) {
            String shortUrl = url.strip();
            if (shortUrl.length() > 38) {
                shortUrl = shortUrl.substring(0, 35) + "...";
            }
            urlLabel.setFont(AcademicTheme.monoFont(Font.PLAIN, 12f));
            urlLabel.setText("<html><a href='#' style='color:#2563EB; text-decoration:underline;'>" + escapeHtml(shortUrl) + "</a></html>");
            urlLabel.setForeground(AcademicTheme.PRIMARY);
            urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            urlLabel.setToolTipText("Buka di browser: " + url.strip());
        } else {
            urlLabel.setFont(AcademicTheme.uiFont(Font.PLAIN, 12f));
            urlLabel.setText("-");
            urlLabel.setForeground(AcademicTheme.TEXT_MUTED);
            urlLabel.setCursor(Cursor.getDefaultCursor());
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

        // Kunci artikel yang diunduh sekarang: jika pengguna memilih artikel lain
        // selagi unduhan berjalan, proses tetap mengunduh artikel yang benar
        Article articleToDownload = currentArticle;

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String resolvedUrl = "";

            @Override
            protected Boolean doInBackground() throws Exception {
                Document doc = Jsoup.connect(articleToDownload.url())
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoCrawl PDF Downloader")
                        .timeout(5000)
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
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    downloadStatusLabel.setText("Gagal mengunduh PDF: " + cause.getMessage());
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
        label.setFont(AcademicTheme.uiFont(Font.BOLD, 11.5f));
        label.setForeground(AcademicTheme.TEXT_MUTED);
        metaFieldLabels.add(label);
        metaLabelColumnWidth = Math.max(metaLabelColumnWidth, label.getPreferredSize().width);
        return label;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Panel khusus yang mengimplementasikan Scrollable agar lebar konten selalu
     * mengikuti lebar viewport secara responsif (tidak melar melebihi batas).
     */
    private static class ResponsiveScrollPanel extends JPanel implements Scrollable {
        public ResponsiveScrollPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(4, 2, 10, 4));
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
