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

import step.artefacts.reports.ThreadReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;
import step.plugins.views.functions.ThreadGroupStatisticsEntry.Statistics;

import java.util.Map.Entry;

@View
public class ThreadGroupStatisticsView extends AbstractTimeBasedGaugeView<ThreadGroupStatisticsEntry> {


	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ThreadGroupStatisticsEntry> model, ReportNode node) {}

	@Override
	public void beforeReportNodeExecution(AbstractTimeBasedModel<ThreadGroupStatisticsEntry> model, ReportNode node) {
		ThreadGroupStatisticsEntry e = createPoint(node);
		if (e != null) {
			addPoint(model, node.getExecutionTime(), e);
		}
	}
	
	private ThreadGroupStatisticsEntry createPoint(ReportNode node) {
		ThreadGroupStatisticsEntry e = null;
		if(node instanceof ThreadReportNode && node.persistNode()) {
			e = new ThreadGroupStatisticsEntry();
			e.count = 1;
			Statistics stats = new Statistics(1);
			e.byThreadGroupName.put(((ThreadReportNode) node).getThreadGroupName(), stats);
		}
		return e;
	}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ThreadGroupStatisticsEntry> model, ReportNode node) {
		ThreadGroupStatisticsEntry e = createPoint(node);
		if (e != null) {
			removePoint(model, node.getExecutionTime()+node.getDuration(), e);
		}
	}

	@Override
	protected ThreadGroupStatisticsEntry getCopy(ThreadGroupStatisticsEntry original) {
		return original.deepCopy();
	}

	@Override
	protected void mergePoints(ThreadGroupStatisticsEntry target, ThreadGroupStatisticsEntry source) {
		target.count += source.count;
		for(Entry<String, Statistics> e:source.byThreadGroupName.entrySet()) {
			Statistics stats = target.byThreadGroupName.get(e.getKey());
			if(stats==null) {
				stats = e.getValue();
			} else {
				stats.count += e.getValue().count;
			}
			target.byThreadGroupName.put(e.getKey(), stats);
		}
	}
	
	@Override
	protected void unMergePoints(ThreadGroupStatisticsEntry target, ThreadGroupStatisticsEntry source) {
		target.count -= source.count;
		for(Entry<String, Statistics> e:source.byThreadGroupName.entrySet()) {
			Statistics stats = target.byThreadGroupName.get(e.getKey());
			if(stats!=null) {
				stats.count -= e.getValue().count;
				target.byThreadGroupName.put(e.getKey(), stats);
			}
		}
	}

	@Override
	public String getViewId() {
		return "ThreadGroupStatistics";
	}

	@Override
	public void rollbackReportNode(AbstractTimeBasedModel<ThreadGroupStatisticsEntry> model, ReportNode node) {
		ThreadGroupStatisticsEntry e = createPoint(node);
		if (e != null) {
			removePoint(model, node.getExecutionTime(), e);
		}
	}
}
