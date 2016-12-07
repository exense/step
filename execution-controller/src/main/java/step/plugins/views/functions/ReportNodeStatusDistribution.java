package step.plugins.views.functions;

import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.views.ViewModel;

public class ReportNodeStatusDistribution extends ViewModel {

	Map<ReportNodeStatus, Entry> progress;

	public ReportNodeStatusDistribution() {
		super();
	}
	
	public ReportNodeStatusDistribution(Map<ReportNodeStatus, Entry> progress) {
		super();
		this.progress = progress;
	}

	public Map<ReportNodeStatus, Entry> getProgress() {
		return progress;
	}

	public void setProgress(Map<ReportNodeStatus, Entry> progress) {
		this.progress = progress;
	}

	public static class Entry {
		
		ReportNodeStatus status;
		
		long count = 0;
		
		long max = 0;

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

		public long getMax() {
			return max;
		}
	}
}
