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

import java.util.Map;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ArtefactHandlerManager {
	
	private final ExecutionContext context;
	
	public ArtefactHandlerManager(ExecutionContext context) {
		super();
		this.context = context;
	}

	public void createReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, ParentSource parentSource) {
		createReportSkeleton(artefact, parentNode, null, parentSource);
	}
	
	public void createReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables, ParentSource parentSource) {
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefact);
		artefactHandler.createReportSkeleton(parentNode, artefact, newVariables, parentSource);
	}

	public ReportNode execute(AbstractArtefact artefact, ReportNode parentNode, ParentSource parentSource) {
		return execute(artefact, parentNode, null, parentSource);
	}

	public ReportNode execute(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables, ParentSource parentSource) {
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefact);
		return artefactHandler.execute(parentNode, artefact, newVariables, parentSource);
	}

	@SuppressWarnings("unchecked")
	public ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(AbstractArtefact artefact) {
		Class<AbstractArtefact> artefactClass = (Class<AbstractArtefact>) artefact.getClass();
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefactClass, context);
		return artefactHandler;
	}
	
	@SuppressWarnings("unchecked")
	private ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(Class<AbstractArtefact> artefactClass, ExecutionContext context) {
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = context.getArtefactHandlerRegistry().getArtefactHandler(artefactClass);
		artefactHandler.init(context);
		return artefactHandler;
	}


}
