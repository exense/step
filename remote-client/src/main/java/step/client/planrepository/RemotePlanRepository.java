package step.client.planrepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.client.accessors.RemoteArtefactAccessor;
import step.client.credentials.ControllerCredentials;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.plans.PlanRepository;

public class RemotePlanRepository implements PlanRepository {

	RemoteArtefactAccessor repositoryClient;

	public RemotePlanRepository(ControllerCredentials credentials) {
		repositoryClient = new RemoteArtefactAccessor(credentials);
	}
	
	public RemotePlanRepository() {
		repositoryClient = new RemoteArtefactAccessor();
	}
	
	@Override
	public Plan load(Map<String, String> attributes) {
		AbstractArtefact root = repositoryClient.findByAttributes(attributes);
		
		List<AbstractArtefact> artefacts = new ArrayList<>();
		loadChildren(artefacts, root);
		
		Plan plan = new Plan(root, artefacts);
		return plan;
	}
	
	@Override
	public void save(Plan plan) {
		plan.getArtefacts().forEach(a->repositoryClient.save(a));
		//repositoryClient.saveArtefactList(new ArrayList<>(plan.getArtefacts()));
	}
	
	private void loadChildren(List<AbstractArtefact> artefacts, AbstractArtefact artefact) {
		artefacts.add(artefact);
		if(artefact.getChildrenIDs()!=null) {
			for(ObjectId childId:artefact.getChildrenIDs()) {
				AbstractArtefact child = repositoryClient.get(childId.toString());
				if(child!=null) {
					loadChildren(artefacts, child);					
				} else {
					throw new RuntimeException("Unable to find artefact with id: "+childId.toString());
				}
			}			
		}
	}
}
