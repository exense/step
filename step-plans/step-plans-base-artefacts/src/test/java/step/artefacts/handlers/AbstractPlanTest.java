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
package step.artefacts.handlers;

import step.artefacts.BaseArtefactPlugin;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.views.ViewPlugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.threadpool.ThreadPoolPlugin;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AbstractPlanTest {
	
	protected ExecutionContext context;
	protected ExecutionEngine executionEngine;

	protected void setupContext() {
		executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
				context = executionContext;
			}
		}).withPlugin(new ViewPlugin()).withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).build();
	}
	
	protected ReportNode execute(AbstractArtefact artefact) {
		Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
		executePlan(plan);
		return getFirstReportNode();
	}

	protected PlanRunnerResult executeArtefact(AbstractArtefact artefact) {
		Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
		return executePlan(plan);
	}

	protected PlanRunnerResult executePlan(Plan plan) {
		return executionEngine.execute(plan);
	}

	protected ReportNode getFirstReportNode() {
		return getReportNodeAccessor().getChildren(context.getReport().getId()).next();
	}
	
	protected List<ReportNode> getChildren(ReportNode node) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(getReportNodeAccessor().getChildren(node.getId()), Spliterator.ORDERED), false).collect(Collectors.toList());
	}

	private ReportNodeAccessor getReportNodeAccessor() {
		return context.getReportNodeAccessor();
	}
}
