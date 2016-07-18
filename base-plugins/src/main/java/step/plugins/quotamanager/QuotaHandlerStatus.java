package step.plugins.quotamanager;

import java.util.ArrayList;
import java.util.List;

import step.plugins.quotamanager.config.Quota;

public class QuotaHandlerStatus {

	Quota configuration;

	int permitsByQuotaKey;
	
	List<QuotaHandlerStatusEntry> entries = new ArrayList<>();
	
	public Quota getConfiguration() {
		return configuration;
	}

	public void addEntry(String quotaKey, int usage, int peak) {
		QuotaHandlerStatusEntry entry = new QuotaHandlerStatusEntry(quotaKey, usage, peak);
		entries.add(entry);
	}
	
	public int getPermitsByQuotaKey() {
		return permitsByQuotaKey;
	}

	public List<QuotaHandlerStatusEntry> getEntries() {
		return entries;
	}

	public class QuotaHandlerStatusEntry {
		
		String quotaKey;
		
		int usage;
		int peak;

		public QuotaHandlerStatusEntry(String quotaKey, int usage, int peak) {
			super();
			this.quotaKey = quotaKey;
			this.usage = usage;
			this.peak = peak;
		}

		public String getQuotaKey() {
			return quotaKey;
		}

		public int getUsage() {
			return usage;
		}
		
		public int getPeak() {
			return peak;
		}
	}
}
