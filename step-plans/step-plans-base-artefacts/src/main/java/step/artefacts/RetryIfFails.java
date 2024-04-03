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
package step.artefacts;

import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.YamlModel;

@YamlModel
@AutomationPackageNamedEntity(name = "retryIfFails")
@Artefact()
public class RetryIfFails extends AbstractArtefact {
	
	DynamicValue<Integer> maxRetries = new DynamicValue<Integer>(1);
		
	DynamicValue<Integer> gracePeriod = new DynamicValue<Integer>(1000);

	DynamicValue<Integer> timeout = new DynamicValue<Integer>(0);
	
	private DynamicValue<Boolean> releaseTokens = new DynamicValue<>(false);
	
	private DynamicValue<Boolean> reportLastTryOnly = new DynamicValue<>(false);
	
	public DynamicValue<Integer> getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(DynamicValue<Integer> maxRetries) {
		this.maxRetries = maxRetries;
	}

	public DynamicValue<Integer> getGracePeriod() {
		return gracePeriod;
	}

	public void setGracePeriod(DynamicValue<Integer> gracePeriod) {
		this.gracePeriod = gracePeriod;
	}

	public DynamicValue<Integer> getTimeout() {
		return timeout;
	}

	public void setTimeout(DynamicValue<Integer> timeout) {
		this.timeout = timeout;
	}

	public DynamicValue<Boolean> getReleaseTokens() {
		return releaseTokens;
	}

	public void setReleaseTokens(DynamicValue<Boolean> releaseTokens) {
		this.releaseTokens = releaseTokens;
	}

	public DynamicValue<Boolean> getReportLastTryOnly() {
		return reportLastTryOnly;
	}

	public void setReportLastTryOnly(DynamicValue<Boolean> reportLastTryOnly) {
		this.reportLastTryOnly = reportLastTryOnly;
	}

}
