package id.gocrawl;

import static id.gocrawl.TestSupport.*;

public class OptionsTest {

    public static void run() {
        System.out.println("\n[TEST SUITE] OptionsTest");

        // 1. Parse Help Flag
        Main.Options helpOpts = Main.Options.parse(new String[]{"--help"});
        assertTrue("Help flag harus aktif", helpOpts.help());

        // 2. Parse List Fields Flag
        Main.Options listOpts = Main.Options.parse(new String[]{"--list-fields"});
        assertTrue("List fields flag harus aktif", listOpts.listFields());

        // 3. Parse Harvester Flags
        Main.Options harvestOpts = Main.Options.parse(new String[]{"--harvest-catalog", "--area", "60", "--limit", "20"});
        assertTrue("Harvest catalog flag aktif", harvestOpts.harvestCatalog());
        assertEquals("Area ID sesuai", "60", harvestOpts.area());
        assertEquals("Limit sesuai", Integer.valueOf(20), harvestOpts.limit());

        // 4. Parse Harvester All Flag
        Main.Options harvestAllOpts = Main.Options.parse(new String[]{"--harvest-catalog", "--all"});
        assertTrue("Harvest all flag aktif", harvestAllOpts.harvestAll());

        // 5. Parse Normal Crawl Flags
        Main.Options crawlOpts = Main.Options.parse(new String[]{
                "--keyword", "kecerdasan buatan, machine learning",
                "--from-year", "2021",
                "--to-year", "2024",
                "--field", "60",
                "--min-sinta", "S2",
                "--direct-ojs"
        });
        assertEquals("Keyword sesuai", "kecerdasan buatan, machine learning", crawlOpts.keyword());
        assertEquals("From year sesuai", Integer.valueOf(2021), crawlOpts.fromYear());
        assertEquals("To year sesuai", Integer.valueOf(2024), crawlOpts.toYear());
        assertEquals("Field sesuai", "60", crawlOpts.area());
        assertEquals("Min SINTA sesuai", "S2", crawlOpts.minSinta());
        assertTrue("Direct OJS aktif", crawlOpts.directOjs());

        // 6. Validasi Error: fromYear > toYear
        boolean caughtYearError = false;
        try {
            Main.Options.parse(new String[]{"--keyword", "test", "--from-year", "2025", "--to-year", "2020"});
        } catch (IllegalArgumentException e) {
            caughtYearError = true;
        }
        assertTrue("Rentang tahun terbalik harus memicu IllegalArgumentException", caughtYearError);

        // 7. Validasi Error: keyword kosong tanpa flag utilitas
        boolean caughtKeywordError = false;
        try {
            Main.Options.parse(new String[]{"--from-year", "2021"});
        } catch (IllegalArgumentException e) {
            caughtKeywordError = true;
        }
        assertTrue("Crawling tanpa --keyword harus memicu IllegalArgumentException", caughtKeywordError);
    }
}
