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
package step.plugins.views.functions;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;

public class ErrorRateViewTest {

	@Test
	public void test() {
		ErrorRateView view = new ErrorRateView();
		view.setResolutions(new int[]{10,100});
		AbstractTimeBasedModel<ErrorRateEntry> model = view.init();
		for(int j=0;j<10;j++) {
			for(int i=0;i<99;i++) {
				ReportNode node = new CallFunctionReportNode();
				CallFunction callFunction = new CallFunction();
				node.setArtefactInstance(callFunction);
				node.setResolvedArtefact(callFunction);
				node.setExecutionTime(j*100+i);
				node.setError("Error "+i%2, 0, true);
				view.afterReportNodeExecution(model, node);
			}
		}
		
		Assert.assertEquals(10,model.getIntervals().size());
		Assert.assertEquals(99,model.getIntervals().get(0l).getCount());
		Assert.assertEquals(99,model.getIntervals().get(900l).getCount());
		Assert.assertEquals(50,(int)model.getIntervals().get(900l).countByErrorMsg.get("Error 0"));
		Assert.assertEquals(49,(int)model.getIntervals().get(900l).countByErrorMsg.get("Error 1"));
	}
}
