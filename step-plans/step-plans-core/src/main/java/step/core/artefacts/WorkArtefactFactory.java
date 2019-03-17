package step.core.artefacts;

import java.util.HashMap;

import org.bson.types.ObjectId;

public class WorkArtefactFactory {
	
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		try {
			T artefact = artefactClass.newInstance();
			if(copyChildren) {
				for(ObjectId childId:parentArtefact.getChildrenIDs()) {
					artefact.addChild(childId);
				}
			}
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put("name", name);
			artefact.setAttributes(attributes);
			return artefact;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

}
