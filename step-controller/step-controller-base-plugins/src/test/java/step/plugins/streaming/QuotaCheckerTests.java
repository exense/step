package step.plugins.streaming;

import org.junit.Test;
import step.streaming.common.QuotaExceededException;

import java.util.UUID;

import static org.junit.Assert.*;

public class QuotaCheckerTests {

    // Helpers ----------------------------------------------------------------

    private static QuotaChecker newChecker(Long maxResourcesPerExecution, Long maxBytesPerResource, Long maxBytesPerExecution) {
        return new QuotaChecker(UUID.randomUUID().toString(), new QuotaLimits(maxBytesPerResource, maxBytesPerExecution, maxResourcesPerExecution, false));
    }

    private static void reserveAndBind(QuotaChecker checker, String resourceId) throws QuotaExceededException {
        String token = checker.reserveNewResource();
        checker.bindResourceId(token, resourceId);
    }

    // Tests ------------------------------------------------------------------

    @Test
    public void reserveAndBindWithinCountQuotaSucceeds() throws Exception {
        QuotaChecker checker = newChecker(2L, 10_000L, 100_000L);
        reserveAndBind(checker, "r1");
        reserveAndBind(checker, "r2");
        // no exception -> success
    }

    @Test
    public void reserveWhenQuotaReachedThrowsQuotaExceeded() throws Exception {
        QuotaChecker checker = newChecker(2L, 10_000L, 100_000L);
        checker.reserveNewResource();
        checker.reserveNewResource();

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, checker::reserveNewResource);
        assertTrue(ex.getMessage().contains("Maximum number of resources"));
    }

    @Test
    public void reserveBindAndCancelWhenNoLimits() throws Exception {
        QuotaChecker checker = newChecker(null, null, null);
        reserveAndBind(checker, "r1");
        String reservation = checker.reserveNewResource();
        checker.cancelReservation(reservation);
    }

    @Test
    public void cancelReservationFreesSlot() throws Exception {
        QuotaChecker checker = newChecker(2L, 10_000L, 100_000L);

        String t1 = checker.reserveNewResource();
        String t2 = checker.reserveNewResource();

        checker.cancelReservation(t2); // free one slot

        String t3 = checker.reserveNewResource(); // should succeed now
        assertNotNull(t3);

        checker.bindResourceId(t1, "r1"); // binding still works
        assertThrows(IllegalStateException.class, () -> checker.bindResourceId(t2, "r2")); // but not with the canceled token
    }

    @Test
    public void bindWithUnknownReservationThrowsIllegalState() {
        QuotaChecker checker = newChecker(2L, 10_000L, 100_000L);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> checker.bindResourceId("no-such-token", "rX"));
        assertTrue(ex.getMessage().startsWith("Reservation token ") && ex.getMessage().endsWith(" not found"));
    }

    @Test
    public void bindDuplicateResourceIdThrowsIllegalState() throws Exception {
        QuotaChecker checker = newChecker(3L, 10_000L, 100_000L);
        String t1 = checker.reserveNewResource();
        checker.bindResourceId(t1, "r1");

        String t2 = checker.reserveNewResource();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> checker.bindResourceId(t2, "r1"));
        assertTrue(ex.getMessage().contains("Resource already bound"));
    }

    @Test
    public void perResourceQuotaExceededThrowsQuotaExceeded() throws Exception {
        QuotaChecker checker = newChecker(3L, 100L, null); // no per-execution cap
        reserveAndBind(checker, "r1");

        checker.onSizeChanged("r1", 100L); // at limit

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.onSizeChanged("r1", 101L));
        assertTrue(ex.getMessage().contains("resource size quota"));
    }

    @Test
    public void perExecutionQuotaEnforcedNoOvershootSingleExecution() throws Exception {
        QuotaChecker checker = newChecker(5L, 10_000L, 150L); // exec cap = 150 bytes
        reserveAndBind(checker, "r1");
        reserveAndBind(checker, "r2");

        checker.onSizeChanged("r1", 100L); // total = 100
        assertEquals(100L, checker.totalBytes.get());
        assertEquals(Long.valueOf(100L), checker.resources.get("r1"));
        assertEquals(Long.valueOf(0L), checker.resources.get("r2"));

        // Would push total to 160 (>150) -> must fail; r2 remains unchanged
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> checker.onSizeChanged("r2", 60L));
        assertTrue(ex.getMessage().contains("reached quota"));
        assertEquals(Long.valueOf(0L), checker.resources.get("r2")); // unchanged
        assertEquals(100L, checker.totalBytes.get()); // unchanged

        // Growing r2 to 50 is allowed (100 + 50 == 150)
        checker.onSizeChanged("r2", 50L);
        assertEquals(150L, checker.totalBytes.get());
        assertEquals(Long.valueOf(50L), checker.resources.get("r2"));
    }

    @Test
    public void totalsUpdateWithMultipleResourcesPerExecution() throws Exception {
        QuotaChecker checker = newChecker(10L, 1_000_000L, 1_000_000L);

        reserveAndBind(checker, "r1");
        reserveAndBind(checker, "r2");
        reserveAndBind(checker, "r3");

        checker.onSizeChanged("r1", 100L);
        checker.onSizeChanged("r2", 200L);
        checker.onSizeChanged("r3", 300L);

        assertEquals(Long.valueOf(100L), checker.resources.get("r1"));
        assertEquals(Long.valueOf(200L), checker.resources.get("r2"));
        assertEquals(Long.valueOf(300L), checker.resources.get("r3"));
        assertEquals(600L, checker.totalBytes.get());

        // Increase one resource; total should reflect the delta
        checker.onSizeChanged("r1", 250L); // +150
        assertEquals(Long.valueOf(250L), checker.resources.get("r1"));
        assertEquals(750L, checker.totalBytes.get());
    }

    @Test
    public void independentTotalsAcrossExecutions() throws Exception {
        // Per-execution cap = 400 bytes; enforce per execution independently
        QuotaChecker exec1 = newChecker(10L, 1_000_000L, 400L);
        QuotaChecker exec2 = newChecker(10L, 1_000_000L, 400L);

        reserveAndBind(exec1, "r1");
        reserveAndBind(exec2, "s1");

        exec1.onSizeChanged("r1", 100L);
        exec2.onSizeChanged("s1", 150L);

        assertEquals(100L, exec1.totalBytes.get());
        assertEquals(150L, exec2.totalBytes.get());

        // Add more resources
        reserveAndBind(exec1, "r2");
        reserveAndBind(exec2, "s2");

        exec1.onSizeChanged("r2", 300L); // exec1 total = 400 (at cap)
        exec2.onSizeChanged("s2", 200L); // exec2 total = 350

        assertEquals(400L, exec1.totalBytes.get());
        assertEquals(350L, exec2.totalBytes.get());

        // Try to exceed exec1 cap by growing r2 -> should fail; exec2 unaffected
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> exec1.onSizeChanged("r2", 350L)); // delta +50 => 450 > 400
        assertTrue(ex.getMessage().contains("reached quota"));
        assertEquals(400L, exec1.totalBytes.get()); // unchanged

        // exec2 can still grow within its own cap
        exec2.onSizeChanged("s2", 240L); // delta +40 => 150 + 240 = 390
        assertEquals(390L, exec2.totalBytes.get());
        assertEquals(400L, exec1.totalBytes.get()); // unchanged
    }

    @Test
    public void decreasingSizeForSingleResourceThrowsIllegalArgument() throws Exception {
        QuotaChecker checker = newChecker(2L, 10_000L, 10_000L);
        reserveAndBind(checker, "r1");

        checker.onSizeChanged("r1", 10L);
        assertThrows(IllegalArgumentException.class, () -> checker.onSizeChanged("r1", 9L));
    }

    @Test
    public void noPerExecutionCapAllowsLargeTotals() throws Exception {
        QuotaChecker checker = newChecker(5L, 1_000_000_000_000L, null); // exec cap disabled
        reserveAndBind(checker, "a");
        reserveAndBind(checker, "b");
        reserveAndBind(checker, "c");

        checker.onSizeChanged("a", 400_000_000_000L);
        checker.onSizeChanged("b", 300_000_000_000L);
        checker.onSizeChanged("c", 350_000_000_000L);
        // no exception -> success; totalBytes is not updated when cap disabled
    }

    @Test
    public void reserveRefusedWhenTotalAlreadyOverCap() {
        QuotaChecker checker = newChecker(10L, 1_000_000L, 150L);
        // Simulate a situation where previous uploads already pushed the total over the (now lower) cap.
        checker.totalBytes.set(160L);

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, checker::reserveNewResource);
        assertTrue(ex.getMessage().contains("reached quota"));
    }

}
