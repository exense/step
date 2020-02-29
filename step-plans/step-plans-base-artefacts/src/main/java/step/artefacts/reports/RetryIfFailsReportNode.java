package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class RetryIfFailsReportNode extends ReportNode {
	private int tries=0;
	private int skipped=0;
	private boolean releasedToken=false;
	
	public int getTries() {
		return tries;
	}
	public void setTries(int tries) {
		this.tries = tries;
	}
	public void incTries() {
		this.tries++;
	}
	public int getSkipped() {
		return skipped;
	}
	public void setSkipped(int skipped) {
		this.skipped = skipped;
	}
	public void incSkipped() {
		this.skipped++;
	}
	public boolean isReleasedToken() {
		return releasedToken;
	}
	public void setReleasedToken(boolean releasedToken) {
		this.releasedToken = releasedToken;
	}
}
