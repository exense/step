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
package step.artefacts.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import step.artefacts.Sequence;
import step.artefacts.While;
import step.artefacts.reports.WhileReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;

public class WhileHandler extends ArtefactHandler<While, WhileReportNode> {

	@Override
	protected void createReportSkeleton_(WhileReportNode parentNode, While testArtefact) {
		evaluateExpressionAndDelegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(WhileReportNode node, While testArtefact) {
		evaluateExpressionAndDelegate(node, testArtefact, true);
	}

	private void evaluateExpressionAndDelegate(WhileReportNode node, While testArtefact, boolean execution) {
		Long timeoutValue = testArtefact.getTimeout().get();
		long timeout = timeoutValue==null?0:timeoutValue;
		long maxTime = System.currentTimeMillis() + timeout;

		Integer maxIterationsValue = testArtefact.getMaxIterations().get();
		int maxIterations = maxIterationsValue==null?0:maxIterationsValue;
		int currIterationsCount = 0;

		int failedLoops = 0;
		
		Long pacingValue = testArtefact.getPacing().get();
		long pacing = pacingValue==null?0:pacingValue;

		List<AbstractArtefact> selectedChildren = getChildren(testArtefact);

		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		
		Map<String, Object> bindings = getBindings();
		String expression = testArtefact.getCondition().getExpression();
		DynamicValue<Boolean> condition = new DynamicValue<>(expression, ""); 
		resolver.evaluate(condition, bindings);
		
		try {
			while(condition.get() 														// expression is true
					&& 		  (timeout == 0 || System.currentTimeMillis() < maxTime)	// infinite Timeout or timeout not reached
					&&  (maxIterations == 0 || currIterationsCount < maxIterations)){	// maxIterations infinite or not reached

				ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
				Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration_"+currIterationsCount);
				iterationTestCase.setPacing(new DynamicValue<Long>(pacing));
				for(AbstractArtefact child:selectedChildren)
					iterationTestCase.addChild(child.getId());

				if(execution){
					ReportNode iterationReportNode = delegateExecute(context, iterationTestCase, node, new HashMap<>());

					if(iterationReportNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || iterationReportNode.getStatus()==ReportNodeStatus.FAILED) {
						failedLoops++;
					}
				}else{
					ArtefactHandler.delegateCreateReportSkeleton(context, iterationTestCase, node);
				}
				
				currIterationsCount++;
				condition = new DynamicValue<>(expression, "");
				bindings = getBindings();
				resolver.evaluate(condition, bindings);
			}
			
			node.setErrorCount(failedLoops);
			node.setCount(currIterationsCount);
			if(failedLoops>0) {
				node.setStatus(ReportNodeStatus.FAILED);
			} else {
				node.setStatus(ReportNodeStatus.PASSED);
			}
			
		} catch (Exception e) {
			failWithException(node, e);
		}
	}

	@Override
	public WhileReportNode createReportNode_(ReportNode parentNode, While testArtefact) {
		return new WhileReportNode();
	}

}
