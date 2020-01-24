package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.CallPlan;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;

public class PlanLocator {
	
	protected ExecutionContext context;
	protected ArtefactAccessor accessor;
	protected SelectorHelper selectorHelper;
	
	public AbstractArtefact selectArtefact(CallPlan testArtefact) {
		AbstractArtefact a;
		
		if(testArtefact.getArtefactId()!=null) {
			a =  accessor.get(testArtefact.getArtefactId());
		} else {
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(testArtefact.getSelectionAttributes().get(), getBindings());
			a = accessor.findRootArtefactByAttributes(selectionAttributes);
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

	public PlanLocator(ExecutionContext context, ArtefactAccessor accessor, SelectorHelper selectorHelper) {
		super();
		this.context = context;
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
	}

}
