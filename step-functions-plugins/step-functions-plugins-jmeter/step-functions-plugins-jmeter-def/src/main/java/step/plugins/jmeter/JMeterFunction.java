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
package step.plugins.jmeter;

import step.automation.packages.AutomationPackageKeyword;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

@AutomationPackageKeyword(name = "JMeter")
public class JMeterFunction extends Function {

	DynamicValue<String> jmeterTestplan = new DynamicValue<>();

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getJmeterTestplan() {
		return jmeterTestplan;
	}

	public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
		this.jmeterTestplan = jmeterTestplan;
	}
}
