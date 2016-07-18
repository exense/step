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
