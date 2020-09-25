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

import java.util.Map.Entry;

import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;

@View
public class ErrorRateView extends AbstractTimeBasedView<ErrorRateEntry> {

	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {}
	
	private ErrorRateEntry createPoint(ReportNode node) {
		ErrorRateEntry e = null;
		if(node.getError()!=null && node.persistNode()) {
			e = new ErrorRateEntry();
			e.count = 1;
			e.countByErrorMsg.put(node.getError().getMsg()==null?"":node.getError().getMsg(), 1);
		}
		return e;
	}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {
		ErrorRateEntry e = createPoint(node);
		if (e != null) {
			addPoint(model, node.getExecutionTime(), e);
		}
	}
	
	@Override
	protected void mergePoints(ErrorRateEntry target, ErrorRateEntry source) {
		target.count+=source.count;
		for(Entry<String, Integer> e:source.countByErrorMsg.entrySet()) {
			Integer count = target.countByErrorMsg.get(e.getKey());
			if(count==null) {
				count = e.getValue();
			} else {
				count = count+e.getValue();
			}
			target.countByErrorMsg.put(e.getKey(), count);
		}
	}

	@Override
	public String getViewId() {
		return "ErrorRate";
	}

	@Override
	public void rollbackReportNode(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {
		ErrorRateEntry e = createPoint(node);
		if (e != null) {
			removePoint(model, node.getExecutionTime(), e);
		}
	}

	@Override
	protected void unMergePoints(ErrorRateEntry target, ErrorRateEntry source) {
		target.count-=source.count;
		for(Entry<String, Integer> e:source.countByErrorMsg.entrySet()) {
			Integer count = target.countByErrorMsg.get(e.getKey());
			if(count!=null) {
				count = count-e.getValue();
				target.countByErrorMsg.put(e.getKey(), count);
			}
			
		}
	}
}
