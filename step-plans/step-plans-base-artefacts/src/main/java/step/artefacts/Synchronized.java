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
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.YamlModel;

@YamlModel
@AutomationPackageNamedEntity(name = "synchronized")
@Artefact()
public class Synchronized extends Sequence {
	
	private DynamicValue<String> lockName = new DynamicValue<String>("");
	private DynamicValue<Boolean> globalLock = new DynamicValue<Boolean>(false);
	
	public DynamicValue<String> getLockName() {
		return lockName;
	}
	
	public void setLockName(DynamicValue<String> lockName) {
		this.lockName = lockName;
	}
	
	public DynamicValue<Boolean> getGlobalLock() {
		return globalLock;
	}
	
	public void setGlobalLock(DynamicValue<Boolean> globalLock) {
		this.globalLock = globalLock;
	}
}
