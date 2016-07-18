package step.plugins.quotamanager.config;


public class Quota {

	private String id;
	
	private String description;
	
	private String quotaKeyFunction;
	
	private int permits;
	
	private Long acquireTimeoutMs;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQuotaKeyFunction() {
		return quotaKeyFunction;
	}

	public void setQuotaKeyFunction(String quotaKeyFunction) {
		this.quotaKeyFunction = quotaKeyFunction;
	}

	public int getPermits() {
		return permits;
	}

	public void setPermits(int permits) {
		this.permits = permits;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getAcquireTimeoutMs() {
		return acquireTimeoutMs;
	}

	public void setAcquireTimeoutMs(long acquireTimeoutMs) {
		this.acquireTimeoutMs = acquireTimeoutMs;
	}

	@Override
	public String toString() {
		return "Quota [id=" + id + ", description=" + description
				+ ", quotaKeyFunction=" + quotaKeyFunction + ", permits="
				+ permits + ", acquireTimeoutMs=" + acquireTimeoutMs + "]";
	}
}
