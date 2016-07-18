package step.plugins.quotamanager.config;

import java.util.List;

public class QuotaManagerConfig {
	
	String id;
	
	List<Quota> quotas;

	public QuotaManagerConfig(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Quota> getQuotas() {
		return quotas;
	}

	public void setQuotas(List<Quota> quotas) {
		this.quotas = quotas;
	}
}
