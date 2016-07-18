package step.core.execution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;

public class InMemoryArtefactAccessor extends ArtefactAccessor {

	Map<ObjectId, AbstractArtefact> map = new HashMap<>();

	@Override
	public AbstractArtefact get(ObjectId artefactID) {
		return map.get(artefactID);
	}

	@Override
	public AbstractArtefact get(String artefactID) {
		return get(new ObjectId(artefactID));
	}

	@Override
	public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
		return parent.getChildrenIDs().stream().map(id->get(id)).iterator();
	}

	@Override
	public AbstractArtefact save(AbstractArtefact artefact) {
		return map.put(artefact.getId(), artefact);
	}

	@Override
	public void save(List<? extends AbstractArtefact> artefacts) {
		artefacts.stream().forEach(artefact->save(artefact));
	}

}
