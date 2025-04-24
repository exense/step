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

import step.artefacts.TestScenario;
import step.artefacts.handlers.functions.MaxAndMultiplyingTokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.pushNewTokenNumberCalculationContext;

public class TestScenarioHandler extends ArtefactHandler<TestScenario, ReportNode> {

	@Override
	public void createReportSkeleton_(ReportNode node, TestScenario testArtefact) {
		TokenForecastingContext tokenForecastingContext = getTokenForecastingContext(context);
		int numberOfThreads = testArtefact.getChildren().size();
		numberOfThreads = context.require(ThreadPool.class).forecastNumberOfThreads(numberOfThreads, OptionalInt.of(numberOfThreads));
		MaxAndMultiplyingTokenForecastingContext newTokenForecastingContext = new MaxAndMultiplyingTokenForecastingContext(tokenForecastingContext, numberOfThreads);
		pushNewTokenNumberCalculationContext(context, newTokenForecastingContext);
		try {
			for(AbstractArtefact child:getChildren(testArtefact)) {
				delegateCreateReportSkeleton(child, node);
				newTokenForecastingContext.nextIteration();
			}
		} finally {
			newTokenForecastingContext.end();
		}
	}

	@Override
	public void execute_(final ReportNode node, TestScenario testArtefact) {
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(node);
		
		List<AbstractArtefact> artefacts = getChildren(testArtefact);
		Iterator<AbstractArtefact> iterator = artefacts.iterator();
		
		ThreadPool threadPool = context.get(ThreadPool.class);
		threadPool.consumeWork(iterator, new WorkerItemConsumerFactory<AbstractArtefact>() {
			@Override
			public Consumer<AbstractArtefact> createWorkItemConsumer(WorkerController<AbstractArtefact> control) {
				return workItem -> {
					ReportNode childReportNode = delegateExecute(workItem, node);
					reportNodeStatusComposer.addStatusAndRecompose(childReportNode);
				};
			}
		}, artefacts.size(), OptionalInt.of(artefacts.size()));

		reportNodeStatusComposer.applyComposedStatusToParentNode(node);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestScenario testArtefact) {
		return new ReportNode();
	}

}
