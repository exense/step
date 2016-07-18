package step.artefacts;

import step.artefacts.handlers.TestGroupHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = TestGroupHandler.class)
public class TestGroup extends AbstractArtefact {
	
	@DynamicAttribute
	private String rampup = null;
	
	@DynamicAttribute
	private String pacing = null;
	
	@DynamicAttribute
	private String users = "1";
	
	@DynamicAttribute
	private String iterations = "1";

	@DynamicAttribute
	private String startOffset = "0";

	@DynamicAttribute
	private String duration = Integer.toString(Integer.MAX_VALUE);

	public String getRampup() {
		return rampup;
	}

	public void setRampup(String rampup) {
		this.rampup = rampup;
	}

	public String getPacing() {
		return pacing;
	}

	public void setPacing(String pacing) {
		this.pacing = pacing;
	}

	public String getUsers() {
		return users;
	}

	public void setUsers(String users) {
		this.users = users;
	}

	public String getIterations() {
		return iterations;
	}

	public void setIterations(String iterations) {
		this.iterations = iterations;
	}

	public String getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(String startOffset) {
		this.startOffset = startOffset;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

}
