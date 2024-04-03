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
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.YamlModel;

@YamlModel
@AutomationPackageNamedEntity(name = "export")
@Artefact(report = ReportNode.class, block=false)
public class Export extends AbstractArtefact {

	private DynamicValue<String> value = new DynamicValue<>("");
	
	private DynamicValue<String> file = new DynamicValue<>("");

	private DynamicValue<String> prefix = new DynamicValue<>("");
	
	private DynamicValue<String> filter = new DynamicValue<>("");
	
	public DynamicValue<String> getValue() {
		return value;
	}

	public void setValue(DynamicValue<String> value) {
		this.value = value;
	}

	public DynamicValue<String> getFile() {
		return file;
	}

	public void setFile(DynamicValue<String> file) {
		this.file = file;
	}

	public DynamicValue<String> getPrefix() {
		return prefix;
	}

	public void setPrefix(DynamicValue<String> prefix) {
		this.prefix = prefix;
	}

	public DynamicValue<String> getFilter() {
		return filter;
	}

	public void setFilter(DynamicValue<String> filter) {
		this.filter = filter;
	}
}
