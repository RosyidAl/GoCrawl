package id.gocrawl.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Token warna, tipografi, dan helper komponen untuk gaya Academic Workbench Engine.
 * Terpusat di sini agar mudah dipelihara dan siap dikembangkan ke Dark Mode di masa depan.
 */
public final class AcademicTheme {

    // --- Permukaan & Latar (Light Mode) ---
    public static final Color SURFACE = new Color(0xF8, 0xF9, 0xFF);
    public static final Color SURFACE_LOW = new Color(0xEF, 0xF4, 0xFF);
    public static final Color SURFACE_CONTAINER = new Color(0xE5, 0xEE, 0xFF);
    public static final Color SURFACE_HIGH = new Color(0xDC, 0xE9, 0xFF);
    public static final Color SURFACE_HIGHEST = new Color(0xD3, 0xE4, 0xFE);
    public static final Color CARD_BG = Color.WHITE;

    // --- Teks ---
    public static final Color ON_SURFACE = new Color(0x0B, 0x1C, 0x30);
    public static final Color ON_SURFACE_VARIANT = new Color(0x43, 0x46, 0x55);
    public static final Color TEXT_MUTED = new Color(0x73, 0x76, 0x86);

    // --- Garis & Batas ---
    public static final Color BORDER_SUBTLE = new Color(0xE2, 0xE8, 0xF0);
    public static final Color BORDER_VARIANT = new Color(0xC3, 0xC6, 0xD7);

    // --- Aksen Utama ---
    public static final Color PRIMARY = new Color(0x25, 0x63, 0xEB);
    public static final Color PRIMARY_HOVER = new Color(0x1D, 0x4E, 0xD8);
    public static final Color PRIMARY_DARK = new Color(0x00, 0x4A, 0xC6);
    public static final Color SELECTION_FILL = new Color(0xEF, 0xF6, 0xFF);
    public static final Color ON_PRIMARY = Color.WHITE;

    // --- Semantik & Diagnostik ---
    public static final Color SECONDARY = new Color(0x00, 0x6A, 0x61);
    public static final Color DANGER = new Color(0xBA, 0x1A, 0x1A);
    public static final Color DANGER_HOVER = new Color(0x93, 0x00, 0x0A);

    // --- Badge Akreditasi SINTA / Scopus ---
    public static final Color S1_TEXT = new Color(0x05, 0x96, 0x69);
    public static final Color S1_BG = new Color(0xEC, 0xFD, 0xF5);
    public static final Color S1_BORDER = new Color(0xA7, 0xF3, 0xD0);

    public static final Color S2_TEXT = new Color(0x0D, 0x94, 0x88);
    public static final Color S2_BG = new Color(0xF0, 0xFD, 0xFA);
    public static final Color S2_BORDER = new Color(0x99, 0xF6, 0xE4);

    public static final Color S34_TEXT = new Color(0xD9, 0x77, 0x06);
    public static final Color S34_BG = new Color(0xFF, 0xFB, 0xEB);
    public static final Color S34_BORDER = new Color(0xFD, 0xE6, 0x8A);

    public static final Color S56_TEXT = new Color(0x47, 0x55, 0x69);
    public static final Color S56_BG = new Color(0xF1, 0xF5, 0xF9);
    public static final Color S56_BORDER = new Color(0xCB, 0xD5, 0xE1);

    public static final Color UNINDEXED_TEXT = new Color(0x94, 0xA3, 0xB8);

    // --- Font Families ---
    private static final String UI_FONT_FAMILY;
    private static final String MONO_FONT_FAMILY;

    static {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        UI_FONT_FAMILY = available.contains("Inter") ? "Inter" : "Segoe UI";
        MONO_FONT_FAMILY = available.contains("JetBrains Mono") ? "JetBrains Mono" : "Consolas";
    }

    private AcademicTheme() {
    }

    public static Font uiFont(int style, float size) {
        return new Font(UI_FONT_FAMILY, style, Math.round(size)).deriveFont(size);
    }

    public static Font monoFont(int style, float size) {
        return new Font(MONO_FONT_FAMILY, style, Math.round(size)).deriveFont(size);
    }

    /** Menerapkan token global FlatLaf untuk seluruh komponen Swing. */
    public static void applyGlobalDefaults() {
        UIManager.put("defaultFont", uiFont(Font.PLAIN, 13f));
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("SplitPane.background", SURFACE);
        UIManager.put("SplitPaneDivider.draggingColor", BORDER_VARIANT);

        // Title bar & Menu bar terang senada sidebar
        UIManager.put("TitlePane.background", SURFACE_LOW);
        UIManager.put("TitlePane.embeddedForeground", ON_SURFACE);
        UIManager.put("TitlePane.buttonHoverBackground", SURFACE_CONTAINER);
        UIManager.put("TitlePane.closeHoverBackground", new Color(232, 17, 35));
        UIManager.put("TitlePane.closePressedBackground", new Color(249, 56, 66));

        UIManager.put("MenuBar.background", SURFACE_LOW);
        UIManager.put("MenuBar.foreground", ON_SURFACE_VARIANT);
        UIManager.put("MenuBar.itemHoverBackground", SURFACE_CONTAINER);
        UIManager.put("MenuBar.borderColor", BORDER_SUBTLE);

        // Input & Kontrol membulat
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.borderColor", BORDER_SUBTLE);

        UIManager.put("TextField.background", SURFACE_LOW);
        UIManager.put("TextField.foreground", ON_SURFACE);
        UIManager.put("TextField.placeholderForeground", TEXT_MUTED);
        UIManager.put("ComboBox.background", SURFACE_LOW);
        UIManager.put("ComboBox.foreground", ON_SURFACE);

        UIManager.put("Table.background", CARD_BG);
        UIManager.put("Table.foreground", ON_SURFACE);
        UIManager.put("Table.selectionBackground", SELECTION_FILL);
        UIManager.put("Table.selectionForeground", ON_SURFACE);
        UIManager.put("Table.gridColor", new Color(0xF1, 0xF5, 0xF9));
        UIManager.put("TableHeader.background", SURFACE_LOW);
        UIManager.put("TableHeader.foreground", TEXT_MUTED);
        UIManager.put("TableHeader.separatorColor", BORDER_SUBTLE);
    }

    /** Menata JTextField dengan gaya input tonal + padding nyaman. */
    public static void styleInput(JTextField field, String placeholder) {
        field.setFont(uiFont(Font.PLAIN, 13f));
        field.setBackground(SURFACE_LOW);
        field.setForeground(ON_SURFACE);
        if (placeholder != null && !placeholder.isBlank()) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
        field.putClientProperty("FlatLaf.style", "arc: 10; borderColor: #E2E8F0; focusedBorderColor: #2563EB; focusWidth: 1");
    }

    /** Menata tombol aksi utama (Cobalt Blue). */
    public static void stylePrimaryButton(JButton btn) {
        btn.setFont(uiFont(Font.BOLD, 13f));
        btn.setBackground(PRIMARY);
        btn.setForeground(ON_PRIMARY);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; hoverBackground: #1D4ED8; pressedBackground: #004AC6");
    }

    /** Menata tombol sekunder / tonal. */
    public static void styleSecondaryButton(JButton btn) {
        btn.setFont(uiFont(Font.BOLD, 12.5f));
        btn.setBackground(SURFACE_CONTAINER);
        btn.setForeground(ON_SURFACE_VARIANT);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; hoverBackground: #DCE9FF");
    }

    /** Menata tombol netral berbingkai tipis (misal Ekspor, Uji Koneksi, Tutup). */
    public static void styleOutlineButton(JButton btn) {
        btn.setFont(uiFont(Font.BOLD, 12f));
        btn.setBackground(CARD_BG);
        btn.setForeground(ON_SURFACE_VARIANT);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("FlatLaf.style", "arc: 10; borderColor: #E2E8F0; hoverBackground: #EFF4FF");
    }

    public static Border cardBorder(int padding) {
        return new CompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(padding, padding, padding, padding)
        );
    }

    /** Memetakan nilai akreditasi ke warna [text, bg, border]. */
    public static Color[] sintaBadgeColors(String rank) {
        if (rank == null || rank.isBlank() || rank.equals("-")) {
            return new Color[]{UNINDEXED_TEXT, Color.WHITE, BORDER_SUBTLE};
        }
        String r = rank.trim().toUpperCase();
        if (r.contains("S1") || r.contains("SINTA 1") || r.contains("SCOPUS")) {
            return new Color[]{S1_TEXT, S1_BG, S1_BORDER};
        }
        if (r.contains("S2") || r.contains("SINTA 2")) {
            return new Color[]{S2_TEXT, S2_BG, S2_BORDER};
        }
        if (r.contains("S3") || r.contains("S4") || r.contains("SINTA 3") || r.contains("SINTA 4")) {
            return new Color[]{S34_TEXT, S34_BG, S34_BORDER};
        }
        if (r.contains("S5") || r.contains("S6") || r.contains("SINTA 5") || r.contains("SINTA 6")) {
            return new Color[]{S56_TEXT, S56_BG, S56_BORDER};
        }
        return new Color[]{UNINDEXED_TEXT, Color.WHITE, BORDER_SUBTLE};
    }
}
