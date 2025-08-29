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
 * Class for keeping track of streaming resource quotas:
 * - per resource size
 * - per execution resource count
 * - per execution total resource size.
 * <p>
 * Instance fields are package-private to facilitate testing, but only public methods should be used from "outside".
 */
public class StreamingQuotaChecker {
    private static final Logger logger = LoggerFactory.getLogger(StreamingQuotaChecker.class);

    private final Long maxBytesPerResource;
    private final Long maxBytesPerExecution;
    private final Integer maxResourcesPerExecution;

    /**
     * resourceId -> executionId (for fast lookup on size updates)
     */
    final ConcurrentHashMap<String, String> resourceToExecution = new ConcurrentHashMap<>();

    /**
     * executionId -> per-execution bucket (package-private for tests)
     */
    final ConcurrentHashMap<String, ExecutionBucket> executionBuckets = new ConcurrentHashMap<>();

    /**
     * Per-execution state. Structural changes are synchronized on this instance.
     */
    static final class ExecutionBucket {
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
        final Set<String> reservations = new HashSet<>();
    }

    public StreamingQuotaChecker(Integer maxResourcesPerExecution, Long maxBytesPerResource, Long maxBytesPerExecution) {
        this.maxResourcesPerExecution = maxResourcesPerExecution;
        this.maxBytesPerExecution = maxBytesPerExecution;
        this.maxBytesPerResource = maxBytesPerResource;
    }

    /**
     * Remove all tracking for an execution.
     */
    public void unregisterExecution(String executionId) {
        ExecutionBucket b = executionBuckets.remove(executionId);
        if (b == null) {
            logger.debug("unregisterExecution: no bucket for execution {}", executionId);
            return;
        }
        for (String resourceId : b.resources.keySet()) {
            resourceToExecution.remove(resourceId, executionId);
        }

        logger.debug("Unregistered execution {} (removed {} active, {} reserved)", executionId, b.resources.size(), b.reservations.size());
    }

    /**
     * Reserve a slot; returns a token to bind later when you have the real resourceId.
     */
    public String reserveNewResource(String executionId) throws QuotaExceededException {
        String token = UUID.randomUUID().toString();

        if (maxResourcesPerExecution == null) {
            logger.debug("reserveNewResource: no count quota enforced; execution={}, token={}", executionId, token);
            return token;
        }

        ExecutionBucket b = executionBuckets.computeIfAbsent(executionId, id -> {
            logger.debug("Created bucket for execution {}", id);
            return new ExecutionBucket();
        });

        synchronized (b) {
            int count = b.resources.size() + b.reservations.size();
            if (count >= maxResourcesPerExecution) {
                String msg = String.format(
                        "Maximum number of resources (%d) for execution %s is reached",
                        maxResourcesPerExecution, executionId);
                logger.debug("reserveNewResource: quota hit for execution {} (active={}, reserved={})",
                        executionId, b.resources.size(), b.reservations.size());
                throw new QuotaExceededException(msg);
            }
            b.reservations.add(token);
            logger.debug("Reserved token {} for execution {} (active={}, reserved={})",
                    token, executionId, b.resources.size(), b.reservations.size());
        }
        return token;
    }

    /**
     * Bind the created resourceId to a previously reserved token. Starts with size=0 and never shrinks.
     */
    public void bindResourceId(String reservation, String executionId, String resourceId) {
        Objects.requireNonNull(resourceId, "resourceId");
        ExecutionBucket b = executionBuckets.computeIfAbsent(executionId, id -> {
            logger.debug("Created bucket for execution {}", id);
            return new ExecutionBucket();
        });
        synchronized (b) {
            if (!b.reservations.remove(reservation)) {
                throw new IllegalStateException("Reservation token not found for execution " + executionId);
            }
            if (b.resources.putIfAbsent(resourceId, 0L) != null) {
                throw new IllegalStateException("Resource already bound: " + resourceId);
            }
            logger.debug("Bound resource {} to execution {} (moved from reserved; active={}, reserved={})",
                    resourceId, executionId, b.resources.size(), b.reservations.size());
        }
        resourceToExecution.put(resourceId, executionId);
    }

    /**
     * Cancel a reservation token (e.g., if creation fails before bind).
     */
    public void cancelReservation(String executionId, String reservation) {
        ExecutionBucket b = executionBuckets.get(executionId);
        if (b == null) {
            logger.debug("cancelReservation: no bucket for execution {}, token={}", executionId, reservation);
            return;
        }
        synchronized (b) {
            boolean removed = b.reservations.remove(reservation);
            if (removed) {
                logger.debug("Canceled reservation token {} for execution {} (active={}, reserved={})",
                        reservation, executionId, b.resources.size(), b.reservations.size());
            } else {
                logger.debug("cancelReservation: token {} not found for execution {}", reservation, executionId);
            }
            cleanupIfEmpty(executionId, b);
        }
    }

    private void cleanupIfEmpty(String executionId, ExecutionBucket b) {
        if (b.resources.isEmpty() && b.reservations.isEmpty()) {
            boolean removed = executionBuckets.remove(executionId, b);
            if (removed) {
                logger.debug("Removed empty bucket for execution {}", executionId);
            }
        }
    }

    /**
     * Monotonic size update (only grows).
     * Enforces per-resource and per-execution quotas strictly (no temporary overshoot).
     * Note: for a given resourceId, updates are expected to be performed by a single thread at a time (no concurrent writers).
     */
    public void onSizeChanged(String resourceId, long currentSize) throws QuotaExceededException {
        if (currentSize < 0) throw new IllegalArgumentException("currentSize must be >= 0");
        checkSizeQuotaPerResource(resourceId, currentSize);

        String executionId = resourceToExecution.get(resourceId);
        if (executionId == null) throw new IllegalStateException("Unexpected: unknown resourceId " + resourceId);

        ExecutionBucket b = executionBuckets.get(executionId);
        if (b == null) throw new IllegalStateException("Unexpected: No bucket for executionId " + executionId);

        Long previousSize = b.resources.get(resourceId); // single-writer per resource => stable during this call
        if (previousSize == null)
            throw new IllegalStateException("Unexpected: Resource " + resourceId + " not tracked under executionId " + executionId);

        if (currentSize < previousSize) {
            throw new IllegalArgumentException("Size update decreased for " + resourceId + ": " + currentSize + " < " + previousSize);
        }

        long delta = currentSize - previousSize;
        if (delta == 0) return; // no-op

        // If there is a per-execution cap, pre-acquire quota (strict cap, no overshoot).
        if (maxBytesPerExecution != null) {
            if (!tryGrow(b.totalBytes, delta, maxBytesPerExecution)) {
                String msg = String.format(
                        "Total size for execution %s would exceed quota %d",
                        executionId, maxBytesPerExecution);
                logger.warn(msg);
                throw new QuotaExceededException(msg);
            }
        }

        // Commit the per-resource size after quota was secured (or immediately if no cap).
        b.resources.put(resourceId, currentSize);
    }
    private void checkSizeQuotaPerResource(String resourceId, long currentSize) throws QuotaExceededException {
        if (maxBytesPerResource != null && currentSize > maxBytesPerResource) {
            String msg = String.format(
                    "Resource %s exceeds per-resource size quota %d, aborting upload",
                    resourceId, maxBytesPerResource);
            logger.warn(msg);
            throw new QuotaExceededException(msg);
        }
    }

    // Lock-free growth of a shared counter by `delta`, capped at `maxAllowed`.
    // Returns true if applied; false if it would exceed the cap.
    private static boolean tryGrow(AtomicLong count, long delta, long maxAllowed) {
        if (delta == 0) return true;                  // no-op
        if (delta < 0) throw new IllegalArgumentException("delta must be >= 0");

        long current;
        for (; ; ) {
            current = count.get();
            long remaining = maxAllowed - current;    // avoids (theoretical) overflow from current + delta
            if (remaining < delta) return false;      // would exceed cap, leave unchanged
            long next = current + delta;              // safe because remaining >= delta
            if (count.compareAndSet(current, next)) {
                return true;                          // success
            }
            // else: raced; retry with fresh current
        }
    }

}
