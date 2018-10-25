package step.core.artefacts;

import java.util.Iterator;
import java.util.Map;

import step.core.accessors.CRUDAccessor;

public interface ArtefactAccessor extends CRUDAccessor<AbstractArtefact> {

	<T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name);

	<T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name, boolean copyChildren);

	/**
	 * Find an artefact by attributes
	 * 
	 * @param attributes the map of mandatory attributes 
	 * @return the artefact
	 */
	AbstractArtefact findByAttributes(Map<String, String> attributes);

	/**
	 * Find a root artefact by attributes. A root artefact is an artefact that is listed in the "Plans" view.
	 * Non root artefacts are the nodes of a Plan
	 * 
	 * @param attributes the map of mandatory attributes 
	 * @return the artefact
	 */
	AbstractArtefact findRootArtefactByAttributes(Map<String, String> attributes);

	AbstractArtefact get(String artefactID);

	Iterator<AbstractArtefact> getRootArtefacts();

	Iterator<AbstractArtefact> getChildren(AbstractArtefact parent);

}