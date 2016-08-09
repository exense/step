package step.artefacts;

import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;


public class AbstractForBlock extends AbstractArtefact {
	
	private String item = "dataPool";
	
	@DynamicAttribute
	private String maxFailedLoops;
	
	@DynamicAttribute
	private String maxLoops;
	
	@DynamicAttribute
	private String parallel = "false";
	
	@DynamicAttribute
	private String threads = "1";
	
	public void setItem(String item) {
		this.item = item;
	}

	public void setMaxFailedLoops(String maxFailedLoops) {
		this.maxFailedLoops = maxFailedLoops;
	}

	public void setMaxLoops(String maxLoops) {
		this.maxLoops = maxLoops;
	}

	public void setParallel(String parallel) {
		this.parallel = parallel;
	}

	public void setThreads(String threads) {
		this.threads = threads;
	}

	public String getItem() {
		return item;
	}

	public String getMaxFailedLoops() {
		return maxFailedLoops;
	}

	public String getMaxLoops() {
		return maxLoops;
	}

	public String getParallel() {
		return parallel;
	}

	public String getThreads() {
		return threads;
	}
}
