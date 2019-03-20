package step.repositories.staging;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.plans.Plan;

public class StagingContext extends AbstractIdentifiableObject {

	protected List<String> attachments = new ArrayList<>();
	protected Plan plan;

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public List<String> getAttachments() {
		return attachments;
	}
	
	public boolean addAttachment(String e) {
		return attachments.add(e);
	}
	
	public void setAttachments(List<String> attachments) {
		this.attachments = attachments;
	}

}