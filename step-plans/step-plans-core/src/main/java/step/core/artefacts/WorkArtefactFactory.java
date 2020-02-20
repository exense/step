package step.core.artefacts;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkArtefactFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkArtefactFactory.class);
	
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		return createWorkArtefact(artefactClass, parentArtefact, name, copyChildren, true);
	}

	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren, boolean persistNode) {
		try {
			T artefact = artefactClass.newInstance();
			if(copyChildren) {
				artefact.setChildren(parentArtefact.getChildren());
			}
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put("name", name);
			artefact.setAttributes(attributes);
			setPersistNodeValue(artefact, persistNode);
			return artefact;
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error("Error while creating new instance of "+artefactClass, e);
			return null;
		}
	}
	
	public void setPersistNodeValue(AbstractArtefact artefact, boolean persistNode) {
		artefact.setPersistNode(persistNode);
		for (AbstractArtefact child : artefact.getChildren()) {
			setPersistNodeValue(child, persistNode);
		}
	}

}
