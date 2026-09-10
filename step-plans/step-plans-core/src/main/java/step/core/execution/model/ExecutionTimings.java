package step.core.execution.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * This class was introduced as part of the JBS-251 feature request,
 * to allow conveying some timing metadata to external systems (in
 * particular, by filling certain Jira Xray fields with the captured
 * timestamps when the execution export is performed).
 * <p>
 * Timestamps are captured and stored in the execution context variables,
 * so they can be accessed in exactly the same way as any other variable.
 * They are saved as ISO-8601 strings, an interoperable de-facto standard
 * that is both computer- and human-readable.
 * <p>
 * The functionality is implemented in the core, primarily because it
 * would have been difficult to implement in a plugin (there are no
 * fine-grained hooks to measure exactly the required timestamps); anyway,
 * this is logic (and information) that won't harm, and might indeed
 * be useful in other contexts as well.
 * <p>
 * See the comments in the individual {@link TimestampVar} fields
 * for information on exactly what timestamp they represent.
 */
public class ExecutionTimings {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionTimings.class);

    private ExecutionTimings() {
    }

    public enum TimestampVar {
        /* Recorded just before plan retrieval / import starts */
        STEP_EXEC_TIMESTAMP_IMPORT,
        /* Recorded just before actual execution starts, after provisioning is completed */
        STEP_EXEC_TIMESTAMP_START,
        /* Recorded right after execution ended, before deprovisioning */
        STEP_EXEC_TIMESTAMP_END,
        /* Recorded right before execution export starts */
        STEP_EXEC_TIMESTAMP_EXPORT,
    }

    public static void recordTimestamp(ExecutionContext context, TimestampVar timing) {
        // Won't happen with current code, but for good measure:
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(timing, "timing must not be null");

        // IS0-8601 format, e.g. 2026-09-08T13:09:54.740868590+02:00
        String isoLocal = OffsetDateTime.now().toString();
        // Variables are saved to the root node
        context.getVariablesManager().putVariable(context.getReport(), timing.name(), isoLocal);
        if (logger.isDebugEnabled()) {
            logger.debug("Recorded timestamp variable: {}={}", timing, isoLocal);
        }
    }
}
