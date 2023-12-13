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
package step.core.execution.table;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.Failure;
import step.artefacts.reports.FailureReportNode;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.model.Execution;

public class RootReportNodeProvider {
				
	protected ReportNodeAccessor reportNodeAccessor;
	
	private static final Logger logger = LoggerFactory.getLogger(RootReportNodeProvider.class);

	public RootReportNodeProvider(GlobalContext context) {
		super();
		reportNodeAccessor = context.getReportAccessor();
	}

	public ReportNode getRootReportNode(Execution execution) {
		String eid = execution.getId().toString();
		
		ReportNode rootReportNode = reportNodeAccessor.getRootReportNode(eid);
		if(rootReportNode!=null) {
			Iterator<ReportNode> rootReportNodeChildren = reportNodeAccessor.getChildren(rootReportNode.getId());
			if(rootReportNodeChildren.hasNext()) {
				rootReportNode = rootReportNodeChildren.next();
				if(rootReportNode != null) {
					return rootReportNode;
				} else {
					logger.error("Error while getting root report node for execution. "
							+ "Iterator.next() returned null although Iterator.hasNext() returned true. "
							+ "This should not occur "+eid);
				}
			} else {
				logger.debug("No children found for report node with id "+rootReportNode.getId());
			}
			return null;
		} else {
			// no root report node present at all, so we assume a failure
			FailureReportNode fn = new FailureReportNode();
			fn.setResolvedArtefact(new Failure());
			fn.setStatus(execution.getResult());
			return fn;
		}
	}
}
