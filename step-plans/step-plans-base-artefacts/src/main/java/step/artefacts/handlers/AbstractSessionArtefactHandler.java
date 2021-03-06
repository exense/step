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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import step.artefacts.FunctionGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

/**
 * Abstract class for {@link ArtefactHandler}s that wrap their child artefacts into {@link FunctionGroup} aka Sessions 
 */
public abstract class AbstractSessionArtefactHandler<ARTEFACT extends AbstractArtefact, REPORT_NODE extends ReportNode> extends ArtefactHandler<ARTEFACT, REPORT_NODE> {

	private static final String DEFAULT_NAME = "Session";

	public AbstractSessionArtefactHandler() {
		super();
	}
	
	protected void createReportNodeSkeletonInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer, String artefactName, Map<String, Object> newVariables) {
		FunctionGroup functionGroup = createWorkArtefact(FunctionGroup.class, artefact, artefactName, true);
		functionGroup.setConsumer(consumer);
		delegateCreateReportSkeleton(functionGroup, node, newVariables);
	}
	
	protected void createReportNodeSkeletonInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer) {
		createReportNodeSkeletonInSession(artefact, node, consumer, DEFAULT_NAME, new HashMap<>());
	}
	
	protected void createReportNodeSkeletonInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer, Map<String, Object> newVariables) {
		createReportNodeSkeletonInSession(artefact, node, consumer, DEFAULT_NAME, newVariables);
	}
	
	/**
	 * Wraps the child artefacts of the provided artefact into a Session and delegates the execution of the Session 
	 * with the provided {@link BiConsumer} (See {@link FunctionGroup#getConsumer})
	 * 
	 * @param artefact the parent artefact 
	 * @param node the report node corresponding to the provided artefact
	 * @param consumer the {@link BiConsumer} representing the action to be performed inside the Session
	 * @param sessionArtefactName the name of the Session artefact to be created
	 * @param newVariables additional variables to be added in the scope of the Session node
	 * @return the {@link ReportNode} corresponding to the Session
	 */
	protected ReportNode executeInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer, String sessionArtefactName, Map<String, Object> newVariables) {
		FunctionGroup functionGroup = createWorkArtefact(FunctionGroup.class, artefact, sessionArtefactName, true);
		functionGroup.setConsumer(consumer);
		ReportNode sessionReportNode = delegateExecute(functionGroup, node, newVariables);
		return sessionReportNode;
	}
	
	protected ReportNode executeInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer) {
		return executeInSession(artefact, node, consumer, DEFAULT_NAME, new HashMap<>());
	}
	
	protected ReportNode executeInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer, Map<String, Object> newVariables) {
		return executeInSession(artefact, node, consumer, DEFAULT_NAME, newVariables);
	}

}
