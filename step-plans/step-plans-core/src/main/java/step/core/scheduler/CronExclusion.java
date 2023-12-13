package step.core.scheduler;

public class CronExclusion {

	private String cronExpression;
	private String description;

	public CronExclusion() {
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
