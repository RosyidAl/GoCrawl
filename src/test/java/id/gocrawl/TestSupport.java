package id.gocrawl;

public class TestSupport {
    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            totalFailed++;
            System.err.println("  ❌ FAILED: " + message);
            throw new AssertionError(message);
        } else {
            totalPassed++;
            System.out.println("  ✅ PASSED: " + message);
        }
    }

    public static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (!match) {
            totalFailed++;
            String failMsg = message + " [Expected: " + expected + ", Actual: " + actual + "]";
            System.err.println("  ❌ FAILED: " + failMsg);
            throw new AssertionError(failMsg);
        } else {
            totalPassed++;
            System.out.println("  ✅ PASSED: " + message);
        }
    }

    public static int getPassed() {
        return totalPassed;
    }

    public static int getFailed() {
        return totalFailed;
    }
}
