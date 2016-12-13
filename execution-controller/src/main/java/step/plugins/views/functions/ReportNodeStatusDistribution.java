package step.plugins.views.functions;

import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.views.ViewModel;

public class ReportNodeStatusDistribution extends ViewModel {

	Map<ReportNodeStatus, Entry> distribution;
	
	long countForecast = 0;
	
	long count = 0;

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
