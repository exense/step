package step.client.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;

public class RemoteArtefactAccessor extends AbstractRemoteClient implements ArtefactAccessor {

	public RemoteArtefactAccessor(ControllerCredentials credentials){
		super(credentials);
	}
	
	public RemoteArtefactAccessor(){
		super();
	}

	@Override
	public void remove(ObjectId id) {
		Builder b = requestBuilder("/rest/controller/artefact/"+id.toString());
		executeRequest(()->b.delete());
	}

	@Override
	public AbstractArtefact save(AbstractArtefact artefact) {
		Builder b = requestBuilder("/rest/controller/artefact");
		Entity<?> entity = Entity.entity(artefact, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, AbstractArtefact.class));
	}

	@Override
	public void save(List<? extends AbstractArtefact> entities) {
		Builder b = requestBuilder("/rest/controller/artefacts");
		Entity<List<?>> entity = Entity.entity(entities, MediaType.APPLICATION_JSON);
		executeRequest(()->b.post(entity));
	}

	@Override
	public AbstractArtefact get(ObjectId id) {
		Builder b = requestBuilder("/rest/controller/artefact/"+id.toString());
		return executeRequest(()->b.get(AbstractArtefact.class));
	}

	@Override
	public Iterator<AbstractArtefact> getAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact,
			String name, boolean copyChildren) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractArtefact findByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder("/rest/controller/artefact/search");
		Entity<?> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, AbstractArtefact.class));
	}

	@Override
	public AbstractArtefact findRootArtefactByAttributes(Map<String, String> attributes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<AbstractArtefact> getRootArtefacts() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractArtefact get(String artefactID) {
		return get(new ObjectId(artefactID));
	}

	@Override
	public Spliterator<AbstractArtefact> findManyByAttributes(Map<String, String> attributes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractArtefact findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Spliterator<AbstractArtefact> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw new UnsupportedOperationException();
	}
}
