package step.plugins.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.streaming.common.QuotaExceededException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-execution quota checker for streaming resources.
 * <p>
 * Enforces:
 * - per resource size
 * - per execution resource count
 * - per execution total resource size
 * <p>
 * Notes:
 * - This instance tracks exactly one execution.
 * - Structural changes are synchronized on 'this'.
 * - Resource sizes are monotonic (single writer per resourceId).
 * - Strict caps: no temporary overshoot on total-bytes.
 * <p>
 * Instance fields are package-private to facilitate testing, but only public methods should be used from "outside".
 */
public class QuotaChecker {
    private static final Logger logger = LoggerFactory.getLogger(QuotaChecker.class);

    private final QuotaLimits limits;

    /**
     * resourceId -> resource size (monotonic, single writer per resource)
     */
    final ConcurrentHashMap<String, Long> resources = new ConcurrentHashMap<>();

    /**
     * Sum of all resource sizes (only updated if maxBytesPerExecution != null)
     */
    final AtomicLong totalBytes = new AtomicLong(0L);

    /**
     * Upcoming uploads which don't have an ID yet
     */
    final Set<String> reservations = ConcurrentHashMap.newKeySet();

    private final String executionId;

    public QuotaChecker(String executionId, QuotaLimits limits) {
        this.executionId = executionId;
        this.limits = limits;
    }

    /**
     * Reserve a slot; returns a token to bind later when you have the real resourceId.
     */
    public String reserveNewResource() throws QuotaExceededException {
        String token = UUID.randomUUID().toString();

        // If total is already above the per-execution cap, refuse new reservations.
        if (limits.maxBytesPerExecution != null && totalBytes.get() >= limits.maxBytesPerExecution) {
            String msg = String.format(
                    "Total size for execution %s reached quota %d",
                    executionId, limits.maxBytesPerExecution);
            logger.debug("reserveNewResource: totalBytes={} exceeds cap {} for execution {}",
                    totalBytes.get(), limits.maxBytesPerExecution, executionId);
            throw new QuotaExceededException(msg);
        }

        if (limits.maxResourcesPerExecution == null) {
            logger.debug("reserveNewResource: no count quota enforced; executionId={}, token={}", executionId, token);
            // this is easier than using special cases (no limits == no reservation) in bind/cancel
            reservations.add(token);
            return token;
        }

        synchronized (this) {
            int count = resources.size() + reservations.size();
            if (count >= limits.maxResourcesPerExecution) {
                String msg = String.format(
                        "Maximum number of resources (%d) for execution %s is reached",
                        limits.maxResourcesPerExecution, executionId);
                logger.debug("reserveNewResource: quota hit for execution {} (active={}, reserved={})",
                        executionId, resources.size(), reservations.size());
                throw new QuotaExceededException(msg);
            }
            reservations.add(token);
            logger.debug("Reserved token {} for execution {} (active={}, reserved={})",
                    token, executionId, resources.size(), reservations.size());
        }
        return token;
    }

    /**
     * Bind the created resourceId to a previously reserved token. Starts with size=0 and never shrinks.
     */
    public void bindResourceId(String reservation, String resourceId) {
        Objects.requireNonNull(resourceId, "resourceId");
        synchronized (this) {
            if (!reservations.remove(reservation)) {
                throw new IllegalStateException("Reservation token " + reservation + " not found");
            }
            if (resources.putIfAbsent(resourceId, 0L) != null) {
                throw new IllegalStateException("Resource already bound: " + resourceId);
            }
            logger.debug("Bound resource {} to execution {} (moved from reserved; active={}, reserved={})",
                    resourceId, executionId, resources.size(), reservations.size());
        }
    }

    /**
     * Cancel a reservation token (e.g., if creation fails before bind).
     */
    public void cancelReservation(String reservation) {
        synchronized (this) {
            boolean removed = reservations.remove(reservation);
            if (removed) {
                logger.debug("Canceled reservation token {} for execution {} (active={}, reserved={})",
                        reservation, executionId, resources.size(), reservations.size());
            } else {
                logger.debug("cancelReservation: token {} not found for execution {}", reservation, executionId);
            }
        }
    }

    /**
     * Monotonic size update (only grows).
     * Enforces per-resource and per-execution quotas.
     * Note: for a given resourceId, updates are expected to be performed by a single thread at a time (no concurrent writers).
     * Because this method is invoked *after* the bytes were already written, we must allow a small overshoot for resources
     * that were accepted for creation before the quota was reached. However, once the (total) quota is reached or exceeded,
     * reservations for new resources will be outright refused
     */
    public void onSizeChanged(String resourceId, long currentSize) throws QuotaExceededException {
        if (currentSize < 0) throw new IllegalArgumentException("currentSize must be >= 0");
        Long previousSize = resources.get(resourceId); // single-writer per resource => stable during this call
        if (previousSize == null)
            throw new IllegalStateException("Unexpected: Resource " + resourceId + " not tracked in this execution");

        long delta = currentSize - previousSize;
        if (delta < 0) {
            throw new IllegalArgumentException("Size update decreased for " + resourceId + " in execution " + executionId + ": " + currentSize + " < " + previousSize);
        }
        if (delta == 0) return; // no-op

        // Always update the resource with current size, writes were already performed anyway.
        resources.put(resourceId, currentSize);

        // In theory, one write can violate both per-resource and per-execution quotas, so make sure all quotas
        // are updated
        QuotaExceededException exception = null;
        try {
            checkSizeQuotaPerResource(resourceId, currentSize);
        } catch (QuotaExceededException e) {
            exception = e;
        }

        // If there is a per-execution cap, acquire quota (may overshoot exactly once).
        if (limits.maxBytesPerExecution != null) {
            if (!grow(totalBytes, delta, limits.maxBytesPerExecution)) {
                String msg = String.format(
                        "Total size for execution %s reached quota %d",
                        executionId, limits.maxBytesPerExecution);
                logger.warn(msg);
                QuotaExceededException ex2 = new QuotaExceededException(msg);
                if (exception == null) {
                    exception = ex2;
                } else {
                    exception.addSuppressed(ex2);
                }
            }
        }
        // throw a single exception (if present)
        if (exception != null) {
            throw exception;
        }

    }

    private void checkSizeQuotaPerResource(String resourceId, long currentSize) throws QuotaExceededException {
        if (limits.maxBytesPerResource != null && currentSize > limits.maxBytesPerResource) {
            String msg = String.format(
                    "Resource %s reached resource size quota %d",
                    resourceId, limits.maxBytesPerResource);
            logger.warn(msg);
            throw new QuotaExceededException(msg);
        }
    }

    // Lock-free growth of a shared counter by `delta`, capped at `maxAllowed`.
    // Returns false if cap is reached or exceeded. Counter is always adjusted though.
    private static boolean grow(AtomicLong count, long delta, long maxAllowed) {
        if (delta == 0) return true;                  // no-op
        if (delta < 0) throw new IllegalArgumentException("delta must be >= 0");

        long current;
        for (; ; ) {
            current = count.get();
            long remaining = maxAllowed - current;    // avoids (theoretical) overflow from current + delta
            long next = current + delta;              // update regardless of quota violation (SED-4352)
            if (count.compareAndSet(current, next)) {
                // returns false if quota reached or exceeded, true if quota not reached
                return remaining >= delta;
            }
            // else: raced; retry with fresh current
        }
    }
}
