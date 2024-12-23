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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import step.artefacts.AbstractArtefactTest;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class AbstractArtefactHandlerTest extends AbstractArtefactTest {
	
	protected ExecutionContext context;
	
	protected void setupContext() {
		context = newExecutionContext();
	}
	
	protected void createSkeleton(AbstractArtefact artefact) {
		createSkeleton(artefact,context.getReport());
	}
	
	protected void createSkeleton(AbstractArtefact artefact, ReportNode parentNode) {
		context.getArtefactHandlerManager().createReportSkeleton(artefact, parentNode, ParentSource.MAIN);
	}
	
	protected ReportNode execute(AbstractArtefact artefact) {
		return execute(artefact,context.getReport());
	}
	
	protected ReportNode execute(AbstractArtefact artefact, ReportNode parentNode) {
		return context.getArtefactHandlerManager().execute(artefact, parentNode, ParentSource.MAIN);
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
	
	protected CheckArtefact newTestArtefact(final ReportNodeStatus status) {
		return new CheckArtefact(c->context.getCurrentReportNode().setStatus(status));
	}
}
