package id.gocrawl.gui;

import id.gocrawl.GarudaSubject;
import id.gocrawl.Journal;
import id.gocrawl.JournalCatalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel Kiri (Sidebar ala Mendeley): Menampilkan hierarki sumber artikel dan jurnal
 * yang telah terindeks sejak awal, serta tombol aksi untuk menambah jurnal baru.
 */
public class SidebarPanel extends JPanel {

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Koleksi");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree tree = new JTree(treeModel);

    private final DefaultMutableTreeNode allArticlesNode = new DefaultMutableTreeNode("Semua Artikel");
    private final DefaultMutableTreeNode indexedJournalsNode = new DefaultMutableTreeNode("Jurnal Terindeks");
    private final DefaultMutableTreeNode garudaSubjectsNode = new DefaultMutableTreeNode("40 Subjek Bidang Ilmu");

    private final JButton addJournalBtn = new JButton("+ Tambah Jurnal Sumber");

    private final JournalCatalog catalog;
    private final Consumer<String> onFilterSelected;

    public SidebarPanel(JournalCatalog catalog, Consumer<String> onFilterSelected) {
        this.catalog = catalog;
        this.onFilterSelected = onFilterSelected;
        initUI();
        refreshTree();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setMinimumSize(new Dimension(240, 400));
        setPreferredSize(new Dimension(270, 600));

        // Tombol Tambah Jurnal di bagian atas sidebar
        JPanel actionBox = new JPanel(new BorderLayout());
        addJournalBtn.setFont(addJournalBtn.getFont().deriveFont(Font.BOLD, 12f));
        addJournalBtn.setPreferredSize(new Dimension(0, 32));
        actionBox.add(addJournalBtn, BorderLayout.CENTER);
        add(actionBox, BorderLayout.NORTH);

        // Hierarki Pohon Sumber
        rootNode.add(allArticlesNode);
        rootNode.add(indexedJournalsNode);
        rootNode.add(garudaSubjectsNode);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRowHeight(24);

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("DIREKTORI SUMBER"));
        add(treeScroll, BorderLayout.CENTER);

        // Event Handlers
        addJournalBtn.addActionListener(e -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            new AddJournalDialog(topFrame, this::refreshTree).setVisible(true);
        });

        tree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path == null) return;
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (selected == null) return;

            Object userObj = selected.getUserObject();
            String nodeText = userObj.toString();

            if (onFilterSelected != null) {
                if (selected == allArticlesNode) {
                    onFilterSelected.accept("ALL");
                } else if (garudaSubjectsNode.isNodeChild(selected)) {
                    onFilterSelected.accept("SUBJECT:" + nodeText);
                } else if (indexedJournalsNode.isNodeChild(selected)) {
                    // Jika node jurnal dipilih, kirim ID atau nama jurnal
                    if (userObj instanceof JournalNode jNode) {
                        onFilterSelected.accept("JOURNAL:" + jNode.journal.name());
                    } else {
                        onFilterSelected.accept("JOURNAL:" + nodeText);
                    }
                }
            }
        });
    }

    public void refreshTree() {
        // 1. Populasi Jurnal Terindeks dari Master Catalog sejak awal
        indexedJournalsNode.removeAllChildren();
        if (catalog != null) {
            List<Journal> journals = catalog.allJournals();
            for (Journal j : journals) {
                indexedJournalsNode.add(new DefaultMutableTreeNode(new JournalNode(j)));
            }
            indexedJournalsNode.setUserObject("Jurnal Terindeks (" + journals.size() + ")");
        } else {
            indexedJournalsNode.setUserObject("Jurnal Terindeks (0)");
        }

        // 2. Populasi 40 Subjek Garuda
        garudaSubjectsNode.removeAllChildren();
        for (var entry : GarudaSubject.OFFICIAL_SUBJECTS.entrySet()) {
            garudaSubjectsNode.add(new DefaultMutableTreeNode(entry.getValue()));
        }

        treeModel.reload();
        // Buka cabang Jurnal Terindeks secara default agar pengguna langsung melihatnya
        tree.expandPath(new TreePath(indexedJournalsNode.getPath()));
    }

    /**
     * Objek representasi node jurnal agar menampilkan Nama Jurnal + Asal Kampus/Penerbit.
     */
    public static class JournalNode {
        public final Journal journal;

        public JournalNode(Journal journal) {
            this.journal = journal;
        }

        @Override
        public String toString() {
            String pub = journal.publisher();
            if (pub != null && !pub.isBlank() && !pub.equals("-")) {
                return journal.name() + " (" + pub + ")";
            }
            return journal.name();
        }
    }
}
