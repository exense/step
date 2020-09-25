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

public class ErrorDistributionViewTest {

	@Test
	public void test() {
		ErrorDistributionView view = new ErrorDistributionView();

		
		ErrorDistribution model = view.init();
		for(int j=0;j<10;j++) {
			for(int i=0;i<100;i++) {
				ReportNode nodePassed = new CallFunctionReportNode();
				CallFunction callFunction = new CallFunction();
				nodePassed.setArtefactInstance(callFunction);
				nodePassed.setResolvedArtefact(callFunction);
				nodePassed.setExecutionTime(j*100+i);
				view.afterReportNodeExecution(model, nodePassed);
				
				ReportNode node = new CallFunctionReportNode();
				node.setArtefactInstance(callFunction);
				node.setResolvedArtefact(callFunction);
				node.setExecutionTime(j*100+i);
				node.setError("Error "+i%2, 0, true);
				view.afterReportNodeExecution(model, node);
			}
		}
		
		Assert.assertEquals(2000,model.count);
		Assert.assertEquals(1000,model.errorCount);
		Assert.assertEquals(2,model.countByErrorMsg.size());
		Assert.assertEquals(500,(int)model.countByErrorMsg.get("Error 0"));
		Assert.assertEquals(500,(int)model.countByErrorMsg.get("Error 1"));
	}
	
	@Test
	public void testOther() {
		ErrorDistributionView view = new ErrorDistributionView();
		
		ErrorDistribution model = view.init();
		for(int j=0;j<1000;j++) {
			ReportNode node = new CallFunctionReportNode();
			CallFunction callFunction = new CallFunction();
			node.setArtefactInstance(callFunction);
			node.setResolvedArtefact(callFunction);
			node.setExecutionTime(j);
			node.setError("Error "+j, 0, true);
			view.afterReportNodeExecution(model, node);
		}
		
		Assert.assertEquals(501,model.getCountByErrorMsg().size());
		Assert.assertEquals(500,(int)model.getCountByErrorMsg().get("Other"));
		
		Assert.assertEquals(1,model.getCountByErrorCode().size());
		Assert.assertEquals(1000,(int)model.getCountByErrorCode().get("0"));
	}
}
