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
package step.core.artefacts.handlers;

import java.util.List;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ParentSource;
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
		createReportSkeleton_(node, testArtefact, ParentSource.MAIN);
	}

	public void createReportSkeleton_(ReportNode node, AbstractArtefact testArtefact, ParentSource parentSource) {
		for (AbstractArtefact child : ArtefactHandler.getChildren(testArtefact, context)) {
			artefactHandlerManager.createReportSkeleton(child, node, parentSource);
		}
	}

	public void createReportSkeleton_(ReportNode node, List<AbstractArtefact> children, ParentSource parentSource) {
		for (AbstractArtefact child : children) {
			artefactHandlerManager.createReportSkeleton(child, node, parentSource);
		}
	}

	public void execute_(ReportNode node, AbstractArtefact testArtefact) {
		execute_(node, testArtefact.getChildren(), null, ParentSource.MAIN);
	}

	public void execute_(ReportNode node, AbstractArtefact testArtefact, ParentSource parentSource) {
		execute_(node, testArtefact.getChildren(), null, parentSource);
	}

	public void execute_(ReportNode reportNode, AbstractArtefact testArtefact, Boolean continueSequenceOnError) {
		execute_(reportNode, testArtefact.getChildren(), continueSequenceOnError, ParentSource.MAIN);
	}
	
	public void execute_(ReportNode reportNode, List<AbstractArtefact> sourceChildren, Boolean continueSequenceOnError, ParentSource parentSource) {
		AtomicReportNodeStatusComposer reportNodeStatusComposer;
		List<AbstractArtefact> children = ArtefactHandler.excludePropertyChildren(ArtefactHandler.getChildrenCopy(sourceChildren, context));
		if (children.isEmpty()) {
			// Set the status to PASSED if the artefact contains no children
			reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.PASSED);
		} else {
			reportNodeStatusComposer = new AtomicReportNodeStatusComposer(reportNode);
		}
		try {
			for (AbstractArtefact child : children) {
				if (context.isInterrupted()) {
					break;
				}
				ReportNode resultNode = artefactHandlerManager.execute(child, reportNode, parentSource);
				reportNodeStatusComposer.addStatusAndRecompose(resultNode);

				if (resultNode.getStatus() == ReportNodeStatus.TECHNICAL_ERROR
						|| resultNode.getStatus() == ReportNodeStatus.FAILED) {
					if (!context.isSimulation()) {
						boolean childContinueOnError = child.getContinueParentNodeExecutionOnError().getOrDefault(false);
						if(childContinueOnError) {
							// continue
							// The value continueParentNodeExecutionOnError of the child element overrides
							// continueSequenceOnError of the sequence
						} else {
							if (continueSequenceOnError != null) {
								if (!continueSequenceOnError) {
									break;
								} else {
									// continue
								}
							} else {
								// break per default
								break;
							}
						}
					}
				}
			}
		} finally {
			if (context.isInterrupted()) {
				reportNode.setStatus(ReportNodeStatus.INTERRUPTED);
			} else {
				reportNodeStatusComposer.applyComposedStatusToParentNode(reportNode);
			}
		}
	}
}
