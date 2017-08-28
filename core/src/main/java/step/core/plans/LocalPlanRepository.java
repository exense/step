package step.core.plans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;

public class LocalPlanRepository implements PlanRepository {

	public LocalPlanRepository(ArtefactAccessor artefactAccessor) {
		super();
		this.artefactAccessor = artefactAccessor;
	}

	// TODO extract interface in order to reuse the implementation of this class in RemotePlanRepository
	ArtefactAccessor artefactAccessor;
	
	@Override
	public Plan load(Map<String, String> attributes) {
		AbstractArtefact root = artefactAccessor.findByAttributes(attributes);
		
		List<AbstractArtefact> artefacts = new ArrayList<>();
		loadChildren(artefacts, root);
		
		Plan plan = new Plan(root, artefacts);
		return plan;
	}
	
	@Override
	public void save(Plan plan) {
		plan.getArtefacts().forEach(a->artefactAccessor.save(a));
	}
	
	private void loadChildren(List<AbstractArtefact> artefacts, AbstractArtefact artefact) {
		artefacts.add(artefact);
		if(artefact.getChildrenIDs()!=null) {
			for(ObjectId childId:artefact.getChildrenIDs()) {
				AbstractArtefact child = artefactAccessor.get(childId.toString());
				if(child!=null) {
					loadChildren(artefacts, child);					
				} else {
					throw new RuntimeException("Unable to find artefact with id: "+childId.toString());
				}
			}			
		}
	}

}
