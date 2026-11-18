package id.gocrawl;

/**
 * Entry point aplikasi desktop GoCrawl.
 *
 * <p>GoCrawl sekarang hanya menyediakan antarmuka GUI. Opsi {@code --gui}
 * dipertahankan sebagai alias eksplisit untuk launcher lama.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && !(args.length == 1 && args[0].equalsIgnoreCase("--gui"))) {
            System.err.println("GoCrawl sekarang berjalan sebagai aplikasi GUI. Jalankan tanpa argumen atau gunakan --gui.");
            System.exit(2);
            return;
        }

        id.gocrawl.gui.GoCrawlGui.launch();
    }
}
