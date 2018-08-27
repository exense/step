package step.core.artefacts;

import java.util.Iterator;
import java.util.Map;

import step.core.accessors.CRUDAccessor;

public interface ArtefactAccessor extends CRUDAccessor<AbstractArtefact> {

	<T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name);

	<T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name, boolean copyChildren);

	AbstractArtefact findByAttributes(Map<String, String> attributes);

	AbstractArtefact findRootArtefactByAttributes(Map<String, String> attributes);

	AbstractArtefact get(String artefactID);

	Iterator<AbstractArtefact> getRootArtefacts();

	Iterator<AbstractArtefact> getChildren(AbstractArtefact parent);

}