/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.functions.plugin;

import java.util.List;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class})
public class FunctionPlugin extends AbstractControllerPlugin {
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		createScreenInputsIfNecessary(context);
	}
	
	protected void createScreenInputsIfNecessary(GlobalContext context) {
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> functionTableInputs = screenInputAccessor.getScreenInputsByScreenId("functionTable");
		functionTableInputs.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				input.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'function'\"/> <function-link function_=\"stBean\" />");
				screenInputAccessor.save(i);
			}
		});
	}
}
