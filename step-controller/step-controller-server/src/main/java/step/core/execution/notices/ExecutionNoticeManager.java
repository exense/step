/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.execution.notices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionNotice;
import step.core.execution.model.ExecutionNoticeSeverity;

import java.util.List;
import java.util.Map;

/**
 * Single, reusable entry point for producing and resolving execution notices. It owns the
 * {@link ExecutionNoticeRegistry} and the {@link ExecutionNoticeResolver}, enforces the per-execution
 * notice cap and registers the built-in "notices suppressed" sentinel type.
 * <p>
 * Producers raise notices via {@link #raiseNotice(ExecutionContext, ExecutionNotice)}; deduplication
 * (if any) is the producer's responsibility. The notice list is otherwise append-only and capped.
 */
public class ExecutionNoticeManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionNoticeManager.class);

    /**
     * Built-in type appended once when the per-execution notice cap is reached.
     */
    public static final String NOTICES_SUPPRESSED_TYPE_ID = "execution.notices-suppressed";

    private final ExecutionNoticeRegistry registry;
    private final ExecutionNoticeResolver resolver;

    /**
     * Maximum number of producer notices per execution. {@code < 0} disables the cap.
     */
    private final int maxNoticesPerExecution;

    public ExecutionNoticeManager(int maxNoticesPerExecution) {
        this.registry = new ExecutionNoticeRegistry();
        this.resolver = new ExecutionNoticeResolver(registry);
        this.maxNoticesPerExecution = maxNoticesPerExecution;
        this.registry.register(new ExecutionNoticeType(NOTICES_SUPPRESSED_TYPE_ID, "system", ExecutionNoticeSeverity.WARNING,
            "Additional execution notices were suppressed because the per-execution limit of {limit} was reached."));
    }

    public ExecutionNoticeRegistry getRegistry() {
        return registry;
    }

    /**
     * Registers a notice type. Convenience delegate to the underlying registry.
     */
    public void register(ExecutionNoticeType type) {
        registry.register(type);
    }

    /**
     * Resolves all notices of the given execution for display.
     */
    public List<ResolvedExecutionNotice> resolve(Execution execution) {
        return resolver.resolve(execution);
    }

    /**
     * Raises a notice on the execution bound to the given context, applying the per-execution cap.
     * No-op when the context is null or the maxNoticesPerExecution is set to 0. Failures are logged and swallowed so notice production never
     * disrupts the execution.
     */
    public void raiseNotice(ExecutionContext executionContext, ExecutionNotice notice) {
        if (executionContext == null || maxNoticesPerExecution == 0) {
            return;
        }
        try {
            executionContext.getExecutionManager().updateExecution(execution -> appendWithCap(execution, notice));
        } catch (Exception e) {
            logger.error("Failed to append notice [{}] to execution {}", notice.getTypeId(), executionContext.getExecutionId(), e);
        }
    }

    /**
     * Appends the notice to the execution unless the cap has been reached. When the cap is first hit,
     * a single sentinel notice is appended instead; further attempts are silently dropped.
     */
    void appendWithCap(Execution execution, ExecutionNotice notice) {
        if (maxNoticesPerExecution > 0) {
            List<ExecutionNotice> existing = execution.getNotices();
            int count = (existing != null) ? existing.size() : 0;
            if (count >= maxNoticesPerExecution) {
                // Append the sentinel exactly once (when the count equals the cap), then stop.
                if (count == maxNoticesPerExecution) {
                    execution.addNotice(new ExecutionNotice(NOTICES_SUPPRESSED_TYPE_ID,
                        Map.of("limit", String.valueOf(maxNoticesPerExecution))));
                }
                return;
            }
        }
        execution.addNotice(notice);
    }
}
