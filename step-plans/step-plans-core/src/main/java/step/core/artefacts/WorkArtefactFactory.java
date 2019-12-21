package step.core.artefacts;

import java.util.HashMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkArtefactFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkArtefactFactory.class);
	
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
			logger.error("Error while creating new instance of "+artefactClass, e);
			return null;
		}
	}

}
