package id.gocrawl.gui;

import com.formdev.flatlaf.FlatLightLaf;
import id.gocrawl.Article;
import id.gocrawl.JournalCatalog;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static id.gocrawl.TestSupport.*;

public class ArticleTablePanelTest {

    public static void run() throws Exception {
        System.out.println("\n[TEST SUITE] ArticleTablePanelTest");

        List<Article> sampleArticles = List.of(
                new Article("Artikel Satu", "Penulis A", "2024", "Jurnal A",
                        "https://example.org/1", "Abstrak satu", "10.1000/1", "2024-01-01", "a"),
                new Article("Artikel Dua", "Penulis B", "2025", "Jurnal B",
                        "https://example.org/2", "Abstrak dua", "10.1000/2", "2025-01-01", "b"),
                new Article("Artikel Tiga", "Penulis C", "2026", "Jurnal C",
                        "https://example.org/3", "Abstrak tiga", "10.1000/3", "2026-01-01", "c")
        );

        List<Article> selectedEvents = new ArrayList<>();
        AtomicInteger clearedEvents = new AtomicInteger(0);
        ArticleTablePanel[] panelRef = new ArticleTablePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            FlatLightLaf.setup();
            ArticleTablePanel panel = new ArticleTablePanel(new JournalCatalog(), selectedEvents::add);
            panel.setOnFilterCleared(clearedEvents::incrementAndGet);
            panel.setArticles(sampleArticles);
            panelRef[0] = panel;
        });

        // Flush invokeLater inside setArticles
        SwingUtilities.invokeAndWait(() -> {});
        SwingUtilities.invokeAndWait(() -> {});

        SwingUtilities.invokeAndWait(() -> {
            ArticleTablePanel panel = panelRef[0];
            assertEquals("Awalnya tidak ada artikel terpilih", null, panel.getSelectedArticle());
            assertTrue("Tombol buang filter harus terlihat", panel.clearFilterButton().isVisible());
            assertFalse("Tombol buang filter harus nonaktif saat filter kosong", panel.clearFilterButton().isEnabled());
            assertFalse("Tombol buang filter tidak boleh mengambil fokus", panel.clearFilterButton().isFocusable());

            // Pilih baris 0 -> membuka artikel
            panel.articleTable().setRowSelectionInterval(0, 0);
            assertEquals("Memilih baris harus memanggil callback 1 kali", 1, selectedEvents.size());
            assertEquals("Artikel terpilih harus Artikel Satu", "Artikel Satu", selectedEvents.get(0).title());

            // Simulasi tutup sidebar kanan via clearSelection()
            panel.clearSelection();
            assertEquals("Setelah clearSelection, tidak ada artikel terpilih", null, panel.getSelectedArticle());
            assertEquals("clearSelection tidak boleh memicu callback tambahan", 1, selectedEvents.size());

            // Saring tabel -> tidak boleh membuka/memilih artikel
            panel.filterField().setText("Artikel Dua");
            assertEquals("Baris terlihat setelah filter harus 1", 1, panel.articleTable().getRowCount());
            assertTrue("Tombol buang filter harus aktif saat filter terisi", panel.clearFilterButton().isEnabled());
            assertEquals("Menyaring tabel tidak boleh memilih artikel", null, panel.getSelectedArticle());
            assertEquals("Menyaring tabel tidak boleh memicu onArticleSelected", 1, selectedEvents.size());

            // Klik tombol buang saringan -> membersihkan filter tanpa membuka artikel
            panel.clearFilterButton().doClick();
            assertEquals("Filter harus kosong setelah tombol diklik", "", panel.filterField().getText());
            assertFalse("Tombol buang filter kembali nonaktif", panel.clearFilterButton().isEnabled());
            assertEquals("Baris terlihat setelah bersihkan filter harus 3", 3, panel.articleTable().getRowCount());
            assertEquals("Membersihkan filter tidak boleh memilih artikel", null, panel.getSelectedArticle());
            assertEquals("Membersihkan filter tidak boleh memicu onArticleSelected", 1, selectedEvents.size());
            assertEquals("Callback onFilterCleared harus terpanggil 1 kali", 1, clearedEvents.get());

            // Skenario: ada baris terpilih lalu user mengetik filter & membersihkan filter
            panel.articleTable().setRowSelectionInterval(1, 1);
            assertEquals("Memilih baris kedua harus memanggil callback", 2, selectedEvents.size());
            panel.filterField().setText("Artikel Dua");
            assertEquals("Menyaring saat baris terpilih harus melepas seleksi", null, panel.getSelectedArticle());
            assertEquals("Menyaring tidak boleh memicu callback baru", 2, selectedEvents.size());
            panel.filterField().setText("");
            assertEquals("Baris kembali 3 setelah filter dikosongkan", 3, panel.articleTable().getRowCount());
            assertEquals("Mengosongkan filter tidak boleh memicu callback pembukaan artikel", 2, selectedEvents.size());
            assertEquals("Tidak ada artikel terpilih setelah filter dikosongkan", null, panel.getSelectedArticle());

            // Uji helper fokus klik di area kosong vs kontrol input
            JPanel emptyPanel = new JPanel();
            JLabel label = new JLabel("Kosong");
            JButton nonFocusableBtn = new JButton("X");
            nonFocusableBtn.setFocusable(false);
            JButton focusableBtn = new JButton("Cari");
            JTextField textField = new JTextField();
            JComboBox<String> combo = new JComboBox<>();
            JMenuItem menuItem = new JMenuItem("Menu");

            assertTrue("Klik JPanel harus melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(emptyPanel, null));
            assertTrue("Klik JLabel harus melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(label, null));
            assertTrue("Klik tombol non-focusable harus melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(nonFocusableBtn, null));
            assertFalse("Klik JTextField tidak boleh melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(textField, null));
            assertFalse("Klik JComboBox tidak boleh melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(combo, null));
            assertFalse("Klik JMenuItem tidak boleh melepas fokus teks",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(menuItem, null));
            assertFalse("Klik tombol focusable tidak boleh melepas fokus sebelum aksi",
                    GoCrawlGui.shouldReleaseTextFocusOnClick(focusableBtn, null));

            if (!GraphicsEnvironment.isHeadless()) {
                GoCrawlGui gui = new GoCrawlGui();
                try {
                    Component initial = gui.getFocusTraversalPolicy().getInitialComponent(gui);
                    assertFalse("Komponen fokus awal GoCrawlGui tidak boleh JTextComponent",
                            initial instanceof JTextComponent);
                } finally {
                    gui.dispose();
                }
            }
        });
    }
}
