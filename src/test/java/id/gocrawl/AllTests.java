package id.gocrawl;

public class AllTests {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("             GOCRAWL COMPREHENSIVE TEST SUITE               ");
        System.out.println("============================================================");

        long startTime = System.currentTimeMillis();

        try {
            GarudaSubjectTest.run();
            JournalCatalogTest.run();
            QualityGateTest.run();
            DeduplicationEngineTest.run();
            TextSanitizerTest.run();
            UrlFilterTest.run();
            GarudaSearchEngineTest.run();
            CitationExporterTest.run();
            id.gocrawl.gui.WorkspaceControllerTest.run();
            id.gocrawl.gui.ArticleExportServiceTest.run();
            id.gocrawl.gui.SearchControllerTest.run();

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n============================================================");
            System.out.println("                      HASIL TESTING                         ");
            System.out.println("============================================================");
            System.out.println("  Status         : SEMUA TEST BERHASIL (SUCCESS)");
            System.out.println("  Total Passed   : " + TestSupport.getPassed() + " assertions");
            System.out.println("  Total Failed   : " + TestSupport.getFailed() + " assertions");
            System.out.println("  Waktu Eksekusi : " + elapsed + " ms");
            System.out.println("============================================================\n");
            System.exit(0);
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("\n============================================================");
            System.err.println("                      HASIL TESTING                         ");
            System.err.println("============================================================");
            System.err.println("  Status         : ADA TEST GAGAL (FAILURE)");
            System.err.println("  Error          : " + t.getMessage());
            System.err.println("  Total Passed   : " + TestSupport.getPassed() + " assertions");
            System.err.println("  Total Failed   : " + TestSupport.getFailed() + " assertions");
            System.err.println("  Waktu Eksekusi : " + elapsed + " ms");
            System.err.println("============================================================\n");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
