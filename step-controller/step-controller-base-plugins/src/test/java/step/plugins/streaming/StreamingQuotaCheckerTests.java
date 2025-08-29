package step.plugins.streaming;

import org.junit.Test;
import step.streaming.common.QuotaExceededException;

import static org.junit.Assert.*;

public class StreamingQuotaCheckerTests {

    private static final String EXEC_1 = "exec-1";
    private static final String EXEC_2 = "exec-2";

    // Helpers ----------------------------------------------------------------

    private static StreamingQuotaChecker newChecker(int maxResourcesPerExecution, long maxBytesPerResource, Long maxBytesPerExecution) {
        return new StreamingQuotaChecker(maxResourcesPerExecution, maxBytesPerResource, maxBytesPerExecution);
    }

    private static void reserveAndBind(StreamingQuotaChecker checker, String executionId, String resourceId) throws QuotaExceededException {
        String token = checker.reserveNewResource(executionId);
        checker.bindResourceId(token, executionId, resourceId);
    }

    // Tests ------------------------------------------------------------------

    @Test
    public void reserveAndBindWithinCountQuotaSucceeds() throws Exception {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 100_000L);
        reserveAndBind(checker, EXEC_1, "r1");
        reserveAndBind(checker, EXEC_1, "r2");
        // no exception -> success
    }

    @Test
    public void reserveWhenQuotaReachedThrowsIOException() throws Exception {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 100_000L);
        checker.reserveNewResource(EXEC_1);
        checker.reserveNewResource(EXEC_1);

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.reserveNewResource(EXEC_1));
        assertTrue(ex.getMessage().contains("Maximum number of resources"));
    }

    @Test
    public void cancelReservationFreesSlot() throws Exception {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 100_000L);

        String t1 = checker.reserveNewResource(EXEC_1);
        String t2 = checker.reserveNewResource(EXEC_1);

        checker.cancelReservation(EXEC_1, t2); // free one slot

        String t3 = checker.reserveNewResource(EXEC_1); // should succeed now
        assertNotNull(t3);

        checker.bindResourceId(t1, EXEC_1, "r1"); // binding still works
        assertThrows(IllegalStateException.class, () -> checker.bindResourceId(t2, EXEC_1, "r2")); // but not with the canceled token
    }

    @Test
    public void bindWithUnknownReservationThrowsIllegalState() {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 100_000L);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> checker.bindResourceId("no-such-token", EXEC_1, "rX"));
        assertTrue(ex.getMessage().contains("Reservation token not found"));
    }

    @Test
    public void bindDuplicateResourceIdThrowsIllegalState() throws Exception {
        StreamingQuotaChecker checker = newChecker(3, 10_000L, 100_000L);
        String t1 = checker.reserveNewResource(EXEC_1);
        checker.bindResourceId(t1, EXEC_1, "r1");

        String t2 = checker.reserveNewResource(EXEC_1);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> checker.bindResourceId(t2, EXEC_1, "r1"));
        assertTrue(ex.getMessage().contains("Resource already bound"));
    }

    @Test
    public void perResourceQuotaExceededThrowsIOException() throws Exception {
        StreamingQuotaChecker checker = newChecker(3, 100L, null); // no per-execution cap
        reserveAndBind(checker, EXEC_1, "r1");

        checker.onSizeChanged("r1", 100L); // at limit

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.onSizeChanged("r1", 101L));
        assertTrue(ex.getMessage().contains("per-resource size quota"));
    }

    @Test
    public void perExecutionQuotaEnforcedNoOvershootSingleExecution() throws Exception {
        StreamingQuotaChecker checker = newChecker(5, 10_000L, 150L); // exec cap = 150 bytes
        reserveAndBind(checker, EXEC_1, "r1");
        reserveAndBind(checker, EXEC_1, "r2");

        checker.onSizeChanged("r1", 100L); // total = 100
        assertEquals(100L, checker.executionBuckets.get(EXEC_1).totalBytes.get());
        assertEquals(Long.valueOf(100L), checker.executionBuckets.get(EXEC_1).resources.get("r1"));
        assertEquals(Long.valueOf(0L), checker.executionBuckets.get(EXEC_1).resources.get("r2"));

        // Would push total to 160 (>150) -> must fail; r2 remains unchanged
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.onSizeChanged("r2", 60L));
        assertTrue(ex.getMessage().contains("would exceed quota"));

        // Growing r2 to 50 is allowed (100 + 50 == 150)
        checker.onSizeChanged("r2", 50L);
        assertEquals(150L, checker.executionBuckets.get(EXEC_1).totalBytes.get());
        assertEquals(Long.valueOf(50L), checker.executionBuckets.get(EXEC_1).resources.get("r2"));
    }

    @Test
    public void totalsUpdateWithMultipleResourcesPerExecution() throws Exception {
        StreamingQuotaChecker checker = newChecker(10, 1_000_000L, 1_000_000L);

        reserveAndBind(checker, EXEC_1, "r1");
        reserveAndBind(checker, EXEC_1, "r2");
        reserveAndBind(checker, EXEC_1, "r3");

        checker.onSizeChanged("r1", 100L);
        checker.onSizeChanged("r2", 200L);
        checker.onSizeChanged("r3", 300L);

        assertEquals(Long.valueOf(100L), checker.executionBuckets.get(EXEC_1).resources.get("r1"));
        assertEquals(Long.valueOf(200L), checker.executionBuckets.get(EXEC_1).resources.get("r2"));
        assertEquals(Long.valueOf(300L), checker.executionBuckets.get(EXEC_1).resources.get("r3"));
        assertEquals(600L, checker.executionBuckets.get(EXEC_1).totalBytes.get());

        // Increase one resource; total should reflect the delta
        checker.onSizeChanged("r1", 250L); // +150
        assertEquals(Long.valueOf(250L), checker.executionBuckets.get(EXEC_1).resources.get("r1"));
        assertEquals(750L, checker.executionBuckets.get(EXEC_1).totalBytes.get());
    }

    @Test
    public void independentTotalsAcrossExecutions() throws Exception {
        // Per-execution cap = 400 bytes; enforce per exec independently
        StreamingQuotaChecker checker = newChecker(10, 1_000_000L, 400L);

        reserveAndBind(checker, EXEC_1, "r1");
        reserveAndBind(checker, EXEC_2, "s1");

        checker.onSizeChanged("r1", 100L);
        checker.onSizeChanged("s1", 150L);

        assertEquals(100L, checker.executionBuckets.get(EXEC_1).totalBytes.get());
        assertEquals(150L, checker.executionBuckets.get(EXEC_2).totalBytes.get());

        // Add more resources
        reserveAndBind(checker, EXEC_1, "r2");
        reserveAndBind(checker, EXEC_2, "s2");

        checker.onSizeChanged("r2", 300L); // EXEC_1 total = 400 (at cap)
        checker.onSizeChanged("s2", 200L); // EXEC_2 total = 350

        assertEquals(400L, checker.executionBuckets.get(EXEC_1).totalBytes.get());
        assertEquals(350L, checker.executionBuckets.get(EXEC_2).totalBytes.get());

        // Try to exceed EXEC_1 cap by growing r2 -> should fail; EXEC_2 unaffected
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.onSizeChanged("r2", 350L)); // delta +50 => 450 > 400
        assertTrue(ex.getMessage().contains("would exceed quota"));

        // EXEC_2 can still grow within its own cap
        checker.onSizeChanged("s2", 240L); // delta +40 => 150 + 240 = 390
        assertEquals(390L, checker.executionBuckets.get(EXEC_2).totalBytes.get());
        assertEquals(400L, checker.executionBuckets.get(EXEC_1).totalBytes.get()); // unchanged
    }

    @Test
    public void decreasingSizeForSingleResourceThrowsIllegalArgument() throws Exception {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 10_000L);
        reserveAndBind(checker, EXEC_1, "r1");

        checker.onSizeChanged("r1", 10L);
        assertThrows(IllegalArgumentException.class, () -> checker.onSizeChanged("r1", 9L));
    }

    @Test
    public void noPerExecutionCapAllowsLargeTotals() throws Exception {
        StreamingQuotaChecker checker = newChecker(5, 1_000_000_000_000L, null); // exec cap disabled
        reserveAndBind(checker, EXEC_1, "a");
        reserveAndBind(checker, EXEC_1, "b");
        reserveAndBind(checker, EXEC_1, "c");

        checker.onSizeChanged("a", 400_000_000_000L);
        checker.onSizeChanged("b", 300_000_000_000L);
        checker.onSizeChanged("c", 350_000_000_000L);
        // no exception -> success; totalBytes not updated when cap disabled, so no assertion here
    }

    @Test
    public void onSizeChangedAfterUnregisterExecutionThrowsIllegalState() throws Exception {
        StreamingQuotaChecker checker = newChecker(2, 10_000L, 10_000L);
        reserveAndBind(checker, EXEC_1, "r1");

        checker.unregisterExecution(EXEC_1);

        assertThrows(IllegalStateException.class, () -> checker.onSizeChanged("r1", 1L));
    }
}
