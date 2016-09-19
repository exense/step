/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.progressunit;

import java.util.HashMap;
import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;

public class Progress {

	volatile int currentProgress;
	
	volatile int maxProgress;
	
	volatile Map<ReportNodeStatus, Integer> reportNodeStatus = new HashMap<ReportNodeStatus, Integer>();
	
	public void setCurrentProgress(int currentProgress) {
		this.currentProgress = currentProgress;
	}

	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

	public Map<ReportNodeStatus, Integer> getReportNodeStatus() {
		return reportNodeStatus;
	}

	public void setReportNodeStatus(Map<ReportNodeStatus, Integer> reportNodeStatus) {
		this.reportNodeStatus = reportNodeStatus;
	}

	public int getCurrentProgress() {
		return currentProgress;
	}

	public int getMaxProgress() {
		return maxProgress;
	}

	public void incrementStatus(ReportNodeStatus status) {
		synchronized (reportNodeStatus) {
			Integer count = reportNodeStatus.get(status);
			if(count==null) {
				reportNodeStatus.put(status, 1);
			} else {
				reportNodeStatus.put(status, count+1);
			}
		}
	}
	
	
}
