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
package step.plugins.views.functions;

import java.util.Map;
import java.util.Map.Entry;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.views.View;
import step.plugins.views.functions.ReportNodeStatisticsEntry.Statistics;

@View
public class ReportNodeStatisticsView extends AbstractTimeBasedView<ReportNodeStatisticsEntry> {

	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {}

	@Override
	public void beforeReportNodeExecution(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {}

	private ReportNodeStatisticsEntry createPoint(ReportNode node) {
		ReportNodeStatisticsEntry e = null;
		if(node instanceof CallFunctionReportNode) {
			e = new ReportNodeStatisticsEntry();
			e.count = 1;
			e.sum = node.getDuration();
			Statistics stats = new Statistics(1, node.getDuration());
			Map<String, String> functionAttributes = ((CallFunctionReportNode)node).getFunctionAttributes();
			if(functionAttributes != null) {
				e.byFunctionName.put(functionAttributes.get(AbstractOrganizableObject.NAME), stats);
			}
		}
		return e;
	}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {
		ReportNodeStatisticsEntry e = createPoint(node);
		if (e != null) {
			addPoint(model, node.getExecutionTime(), e);
		}
	}
	
	@Override
	protected void mergePoints(ReportNodeStatisticsEntry target, ReportNodeStatisticsEntry source) {
		target.count+=source.count;
		target.sum+=source.sum;
		for(Entry<String, Statistics> e:source.byFunctionName.entrySet()) {
			Statistics stats = target.byFunctionName.get(e.getKey());
			if(stats==null) {
				stats = e.getValue();
			} else {
				stats.count+=e.getValue().count;
				stats.sum+=e.getValue().sum;
			}
			target.byFunctionName.put(e.getKey(), stats);
		}
	}
	
	@Override
	protected void unMergePoints(ReportNodeStatisticsEntry target, ReportNodeStatisticsEntry source) {
		target.count-=source.count;
		target.sum-=source.sum;
		for(Entry<String, Statistics> e:source.byFunctionName.entrySet()) {
			Statistics stats = target.byFunctionName.get(e.getKey());
			if(stats!=null) {
				stats.count-=e.getValue().count;
				stats.sum-=e.getValue().sum;
				target.byFunctionName.put(e.getKey(), stats);
			}
		}
	}

	@Override
	public String getViewId() {
		return "ReportNodeStatistics";
	}

	@Override
	public void onReportNodeRemoval(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {
		ReportNodeStatisticsEntry e = createPoint(node);
		if (e != null) {
			removePoint(model, node.getExecutionTime(), e);
		}
	}

	@Override
	public void onErrorContributionRemoval(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {
		// Nothing to be done here as the statistics don't trace errors
	}
}
