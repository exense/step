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

import step.artefacts.TestSet;
import step.artefacts.handlers.functions.MaxAndMultiplyingTokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.pushNewTokenNumberCalculationContext;

public class TestSetHandler extends ArtefactHandler<TestSet, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestSet testSet) {	
		context.getExecutionManager().updateExecutionType("TestSet");

		TokenForecastingContext tokenForecastingContext = getTokenForecastingContext(context);

		int threads = context.require(ThreadPool.class).getEffectiveNumberOfThreads(getNumberThreads(testSet), getChildren(testSet).size());

		MaxAndMultiplyingTokenForecastingContext newTokenForecastingContext = new MaxAndMultiplyingTokenForecastingContext(tokenForecastingContext, threads);
		pushNewTokenNumberCalculationContext(context, newTokenForecastingContext);
		try {
			for(AbstractArtefact child:getChildren(testSet)) {
				delegateCreateReportSkeleton(child, node);
				newTokenForecastingContext.nextIteration();
			}
		} finally {
			newTokenForecastingContext.end();
		}
	}

	private int getNumberThreads(TestSet testSet) {
		int threads = testSet.getThreads().get();

		if(threads == 0) {
			threads = context.getVariablesManager().getVariableAsInteger("tec.execution.threads", 1);
		}
		return threads;
	}

	@Override
	public void execute_(ReportNode node, TestSet testSet) {
		runParallel(node, testSet, true);
	}

	private void runParallel(ReportNode node, TestSet testSet, boolean execution) {
		int numberOfThreads = getNumberThreads(testSet);

		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.NORUN);
		
		List<AbstractArtefact> children = getChildren(testSet);
		Iterator<AbstractArtefact> childrenIt = children.iterator();
		ThreadPool threadPool = context.get(ThreadPool.class);
		int effectiveNumberOfThreads = threadPool.getEffectiveNumberOfThreads(numberOfThreads, children.size());

		threadPool.consumeWork(childrenIt, new WorkerItemConsumerFactory<AbstractArtefact>() {
			@Override
			public Consumer<AbstractArtefact> createWorkItemConsumer(WorkerController<AbstractArtefact> control) {
				return workItem -> {
					if(execution) {
						ReportNode resultNode = delegateExecute(workItem, node);
						reportNodeStatusComposer.addStatusAndRecompose(resultNode);
					} else {
						delegateCreateReportSkeleton(workItem, node);
					}
				};
			}
		}, effectiveNumberOfThreads);

		reportNodeStatusComposer.applyComposedStatusToParentNode(node);
	}
	
	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestSet testArtefact) {
		return new ReportNode();
	}
}
