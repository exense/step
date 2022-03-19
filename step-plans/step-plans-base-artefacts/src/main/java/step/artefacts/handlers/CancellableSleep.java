package step.artefacts.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class CancellableSleep {
    private static final Logger logger = LoggerFactory.getLogger(CancellableSleep.class);
    public static final long DEFAULT_CHECK_INTERVAL_MS = 1000;

    private CancellableSleep() {
    }

    public static boolean sleep(long duration, Supplier<Boolean> cancelCondition, Class<?> invokingClass) {
        return sleep(duration, cancelCondition, DEFAULT_CHECK_INTERVAL_MS, invokingClass.getSimpleName());
    }

    /**
     * Sleeps for a maximum of <tt>duration</tt> milliseconds, checking every <tt>checkIntervalMs</tt> milliseconds
     * whether the <tt>cancelCondition</tt> is met, and cancelling the sleeping if so.
     *
     * @param duration          number of milliseconds to sleep
     * @param cancelCondition   method which is invoked to verify if execution is to be cancelled.
     *                          This method is invoked often, and in time-sensitive code, so it should return quickly.
     * @param checkIntervalMs   number of milliseconds between cancellation checks.
     *                          A lower limit of 10 ms is enforced, but higher values (say, >= 100 ms) are strongly recommended.
     * @param invocationContext indication of the context where the method invocation originated.
     *                          Only used for logging, and may be null.
     * @return <tt>true</tt> if the entire sleep went through without being cancelled, <tt>false</tt> if it was cancelled.
     */
    public static boolean sleep(final long duration, final Supplier<Boolean> cancelCondition, final long checkIntervalMs, final String invocationContext) {
        final long startTimestamp = System.currentTimeMillis();
        final long endTimestamp = startTimestamp + duration;

        if (checkIntervalMs < 10) {
            throw new IllegalArgumentException("checkIntervalMs must be at least 10 milliseconds");
        }

        final String context = makeLogContextString(invocationContext);

        if(logger.isDebugEnabled()) {
        	logger.debug(context + "Sleeping for " + duration + " ms; checking for cancellation every " + checkIntervalMs + " ms");
        }

        while (true) {
            if (cancelCondition.get()) {
                long actualDuration = System.currentTimeMillis() - startTimestamp;
                if(logger.isDebugEnabled()) {
                    logger.debug(context + "Cancelled sleeping; intended duration=" + duration + ", actual duration=" + actualDuration);
                }
                return false;
            }

            long now = System.currentTimeMillis();
            long leftToSleep = endTimestamp - now;
            if (leftToSleep <= 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug(context + "Finished sleeping; requested duration=" + duration + ", actual duration=" + (now - startTimestamp));
                }
                return true;
            }

            long chunk;
            if(leftToSleep >= checkIntervalMs) {
            	chunk = checkIntervalMs;
            } else {
            	// Sleep at least 1ms to avoid tightly looping multiple times during the same millisecond
            	chunk = Math.max(1, leftToSleep % checkIntervalMs);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(context + "Sleeping for another " + chunk + " ms, total ms left to sleep: " + leftToSleep);
            }
            try {
                Thread.sleep(chunk);
            } catch (InterruptedException e) {
                logger.warn(context + "Thread was interrupted while sleeping, continuing nevertheless", e);
            }
        }
    }

    private static String makeLogContextString(String invocationContext) {
        if (invocationContext == null) {
            return "";
        }
        return "[" + invocationContext + "] ";
    }

}
