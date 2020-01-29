package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.CallPlan;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class PlanLocator {
	
	protected ExecutionContext context;
	protected PlanAccessor accessor;
	protected SelectorHelper selectorHelper;
	
	public Plan selectPlan(CallPlan artefact) {
		Plan a;
		
		if(artefact.getPlanId()!=null) {
			a =  accessor.get(artefact.getPlanId());
		} else {
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(artefact.getSelectionAttributes().get(), getBindings());
			a = accessor.findByAttributes(selectionAttributes);
			if(a==null) {
				throw new RuntimeException("Unable to find plan with attributes: "+selectionAttributes.toString());
			}
		}
		return a;
	}
	
	private Map<String, Object> getBindings() {
		if (context != null) {
			return ExecutionContextBindings.get(context);
		} else {
			return null;
		}
	}

	public PlanLocator(ExecutionContext context, PlanAccessor accessor, SelectorHelper selectorHelper) {
		super();
		this.context = context;
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
	}

}
