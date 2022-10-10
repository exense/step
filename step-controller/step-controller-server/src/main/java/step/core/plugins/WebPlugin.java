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
package step.core.plugins;

import java.util.ArrayList;
import java.util.List;

public class WebPlugin extends AbstractWebPlugin {

	List<String> scripts = new ArrayList<>();
	
	List<String> angularModules = new ArrayList<>();

	public WebPlugin() {
		super();
	}

	public void setScripts(List<String> scripts) {
		this.scripts = scripts;
	}

	public void setAngularModules(List<String> angularModules) {
		this.angularModules = angularModules;
	}

	public List<String> getScripts() {
		return scripts;
	}

	public List<String> getAngularModules() {
		return angularModules;
	}
}
