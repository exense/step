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

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import step.artefacts.TestSet;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionTypeListener;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

public class TestSetHandler extends ArtefactHandler<TestSet, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestSet testSet) {	
		ExecutionTypeListener executionTypeListener = context.getExecutionManager();
		executionTypeListener.updateExecutionType(context, "TestSet");
		runParallel(node, testSet, false);
	}

	@Override
	public void execute_(ReportNode node, TestSet testSet) {
		runParallel(node, testSet, true);
	}

	private void runParallel(ReportNode node, TestSet testSet, boolean execution) {
		int numberOfThreads = testSet.getThreads().get();
		
		if(numberOfThreads == 0) {
			numberOfThreads = context.getVariablesManager().getVariableAsInteger("tec.execution.threads", 1);
		}
		
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.NORUN);
		
		List<AbstractArtefact> children = getChildren(testSet);
		Iterator<AbstractArtefact> childrenIt = children.iterator();
		
		ThreadPool threadPool = context.get(ThreadPool.class);
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
		}, numberOfThreads, OptionalInt.of(children.size()));

		reportNodeStatusComposer.applyComposedStatusToParentNode(node);
	}
	
	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestSet testArtefact) {
		return new ReportNode();
	}
}
