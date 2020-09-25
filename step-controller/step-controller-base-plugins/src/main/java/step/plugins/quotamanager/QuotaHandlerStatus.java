/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
