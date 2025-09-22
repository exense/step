/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.livereporting;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin(dependencies= {})
public class LiveReportingPlugin extends AbstractExecutionEnginePlugin {

    public static final String LIVE_REPORTING_CONTEXT = "$liveReportingContext";
    private LiveReportingContexts liveReportingContexts;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        liveReportingContexts = parentContext != null ? parentContext.get(LiveReportingContexts.class) : null;
        if (liveReportingContexts == null) {
            // TODO set up live reporting for local executions
            liveReportingContexts = new LiveReportingContexts("");
        }
        executionEngineContext.put(LiveReportingContexts.class, liveReportingContexts);
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        executionContext.put(LiveReportingContexts.class, liveReportingContexts);
    }

    @Override
    public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
        if(node instanceof CallFunctionReportNode) {
            LiveReportingContext reportingContext = liveReportingContexts.createReportingContext();
            context.getVariablesManager().putVariable(node, LIVE_REPORTING_CONTEXT, reportingContext);
        }
    }

    @Override
    public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
        if(node instanceof CallFunctionReportNode) {
            LiveReportingContext liveReportingContext = LiveReportingPlugin.getLiveReportingContext(context);
            liveReportingContexts.unregister(liveReportingContext.id);
        }
    }

    public static LiveReportingContext getLiveReportingContext(ExecutionContext executionContext) {
        return (LiveReportingContext) executionContext.getVariablesManager().getVariable(LIVE_REPORTING_CONTEXT);
    }
}
