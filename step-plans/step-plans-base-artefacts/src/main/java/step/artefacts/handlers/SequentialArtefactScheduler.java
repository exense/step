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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import step.artefacts.AfterSequence;
import step.artefacts.BeforeSequence;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class SequentialArtefactScheduler {

	private final ExecutionContext context;
	private ArtefactHandlerManager artefactHandlerManager;
	
	public SequentialArtefactScheduler(ExecutionContext context) {
		super();
		this.context = context;
		artefactHandlerManager = context.getArtefactHandlerManager();
	}

	public void createReportSkeleton_(ReportNode node, AbstractArtefact testArtefact) {
		for (AbstractArtefact child : ArtefactHandler.getChildren(testArtefact, context)) {
			artefactHandlerManager.createReportSkeleton(child, node);
		}
	}

	public void execute_(ReportNode node, AbstractArtefact testArtefact) {
		execute_(node, testArtefact, null);
	}

	/**
	 * Delegates the execution of the children artefacts of the provided artefact within the BeforeSequence and AfterSequence artefacts
	 * of the provided artefact.
	 * 
	 * @param artefact the artefact to be executed
	 * @param reportNode the {@link ReportNode} corresponding to the provided artefact
	 * @param consumer the consumer to delegate the execution of the children artefacts without Before and AfterSequence artefacts
	 * @return the {@link ReportNode} of the provided artefact
	 */
	public ReportNode executeWithinBeforeAndAfter(AbstractArtefact artefact, ReportNode reportNode, Function<List<AbstractArtefact>, ReportNode> consumer) {
		return executeWithinBeforeAndAfter(artefact, reportNode, consumer, BeforeSequence.class, AfterSequence.class);
	}
	
	/**
	 * Delegates the execution of the children artefacts of the provided artefact within the Before and After artefacts
	 * of the provided artefact.
	 * 
	 * @param artefact the artefact to be executed
	 * @param reportNode the {@link ReportNode} corresponding to the provided artefact
	 * @param consumer the consumer to delegate the execution of the children artefacts without Before and AfterSequence artefacts
	 * @param beforeClass the class of the before artefacts
	 * @param afterClass the class of the after artefacts
	 * @return
	 */
	public ReportNode executeWithinBeforeAndAfter(AbstractArtefact artefact, ReportNode reportNode, Function<List<AbstractArtefact>, ReportNode> consumer, 
			Class<? extends AbstractArtefact> beforeClass, Class<? extends AbstractArtefact> afterClass) {
		List<AbstractArtefact> children = ArtefactHandler.getChildren(artefact, context);
		
		// get the list of BeforeSequence artefacts that have to be executed before the sequence 
		List<AbstractArtefact> beforeArtefacts = children.stream().filter(c->beforeClass.isInstance(c)).collect(Collectors.toList());
		// get the list of AfterSequence artefacts that have to be executed after the sequence
		List<AbstractArtefact> afterArtefacts = children.stream().filter(c->afterClass.isInstance(c)).collect(Collectors.toList());
		// get the list of children artefacts without Before- and AfterSequence artefacts
		List<AbstractArtefact> newChildren = children.stream().filter(c->!(beforeClass.isInstance(c)) && !(afterClass.isInstance(c))).collect(Collectors.toList());
		
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(reportNode.getStatus());
		try {
			// Execute the BeforeSequence artefacts
			for (AbstractArtefact beforeArtefact : beforeArtefacts) {
				ReportNode resultNode = artefactHandlerManager.execute(beforeArtefact, reportNode);
				reportNodeStatusComposer.addStatusAndRecompose(resultNode.getStatus());
			}
			
			// delegate the execution of the new children without Before- and AfterSequence artefacts
			ReportNode resultNode = consumer.apply(newChildren);
			reportNodeStatusComposer.addStatusAndRecompose(resultNode.getStatus());
		} finally {
			// Execute the AfterSequence artefacts
			for (AbstractArtefact afterArtefact : afterArtefacts) {
				ReportNode resultNode = artefactHandlerManager.execute(afterArtefact, reportNode);
				reportNodeStatusComposer.addStatusAndRecompose(resultNode.getStatus());
			}
		}
		reportNode.setStatus(reportNodeStatusComposer.getParentStatus());
		return reportNode;
	}
	
	public void execute_(ReportNode reportNode, AbstractArtefact artefact, Boolean continueOnError) {
		executeWithinBeforeAndAfter(artefact, reportNode, children -> {
			AtomicReportNodeStatusComposer reportNodeStatusComposer;
			if (children.isEmpty()) {
				// Set the status to PASSED if the artefact contains no children
				reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.PASSED);
			} else {
				reportNodeStatusComposer = new AtomicReportNodeStatusComposer(reportNode.getStatus());
			}
			try {
				for (AbstractArtefact child : children) {
					if (context.isInterrupted()) {
						break;
					}
					ReportNode resultNode = artefactHandlerManager.execute(child, reportNode);
					reportNodeStatusComposer.addStatusAndRecompose(resultNode.getStatus());
	
					if (resultNode.getStatus() == ReportNodeStatus.TECHNICAL_ERROR
							|| resultNode.getStatus() == ReportNodeStatus.FAILED) {
						if (!context.isSimulation()) {
							if (continueOnError != null) {
								if (!continueOnError) {
									break;
								}
							} else {
								break;
							}
						}
					}
				}
			} finally {
				if (context.isInterrupted()) {
					reportNode.setStatus(ReportNodeStatus.INTERRUPTED);
				} else {
					reportNode.setStatus(reportNodeStatusComposer.getParentStatus());
				}
			}
			return reportNode;
		});
	}
}
