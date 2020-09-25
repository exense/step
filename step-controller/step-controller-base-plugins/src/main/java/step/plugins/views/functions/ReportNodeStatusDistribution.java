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

import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.views.ViewModel;

public class ReportNodeStatusDistribution extends ViewModel {

	Map<ReportNodeStatus, Entry> distribution;
	
	long countForecast = 0;
	
	long count = 0;
	
	String label;

	public long getCountForecast() {
		return countForecast;
	}

	public long getCount() {
		return count;
	}

	public ReportNodeStatusDistribution() {
		super();
	}
	
	public ReportNodeStatusDistribution(Map<ReportNodeStatus, Entry> progress) {
		super();
		this.distribution = progress;
	}

	public Map<ReportNodeStatus, Entry> getDistribution() {
		return distribution;
	}

	public void setDistribution(Map<ReportNodeStatus, Entry> progress) {
		this.distribution = progress;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public static class Entry {
		
		ReportNodeStatus status;
		
		long count = 0;

		public Entry() {
			super();
		}

		public Entry(ReportNodeStatus status) {
			super();
			this.status = status;
		}

		public ReportNodeStatus getStatus() {
			return status;
		}

		public void setStatus(ReportNodeStatus status) {
			this.status = status;
		}

		public long getCount() {
			return count;
		}
	}
}
