package step.migration.tasks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.artefacts.CallPlan;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.ArtefactAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionAccessorImpl;
import step.migration.MigrationTask;

/**
 * This function ensures that all the artefacts have their name saved properly in the attribute map. 
 * This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
 *
 */
public class SetArtefactNamesIfEmpty extends MigrationTask {

	public SetArtefactNamesIfEmpty() {
		super(new Version(3,4,0));
	}

	@Override
	public void runUpgradeScript() {
		FunctionAccessor functionRepository = new FunctionAccessorImpl(context.getMongoClientSession());
		
		ArtefactAccessor a = context.getArtefactAccessor();
		
		AbstractArtefact artefact;
		Iterator<AbstractArtefact> it = a.getAll();
		while(it.hasNext()) {
			artefact = it.next();
			Map<String,String> attributes = artefact.getAttributes();
			if(attributes==null) {
				attributes = new HashMap<>();
				artefact.setAttributes(attributes);
			}
			
			if(!attributes.containsKey("name")) {
				String name = null;
				if(artefact instanceof CallFunction) {
					CallFunction calllFunction = (CallFunction) artefact;
					if(calllFunction.getFunctionId()!=null) {
						Function function = functionRepository.get(new ObjectId(calllFunction.getFunctionId()));
						if(function!=null && function.getAttributes()!=null && function.getAttributes().containsKey(Function.NAME)) {
							name = function.getAttributes().get(Function.NAME);
						}						
					}
				} else if(artefact instanceof CallPlan) {
					CallPlan callPlan = (CallPlan) artefact;
					if(callPlan.getArtefactId()!=null) {
						AbstractArtefact calledArtefact = a.get(callPlan.getArtefactId());
						if(calledArtefact != null && calledArtefact.getAttributes()!=null && calledArtefact.getAttributes().containsKey("name")) {
							name = calledArtefact.getAttributes().get("name");
						}						
					}
				}
				if(name == null) {
					Artefact annotation = artefact.getClass().getAnnotation(Artefact.class);
					name =  annotation.name().length() > 0 ? annotation.name() : artefact.getClass().getSimpleName();
				}
				attributes.put("name", name);
				a.save(artefact);
			}
		}
	}

	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub
		
	}

}
