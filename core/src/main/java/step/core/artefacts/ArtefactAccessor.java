package step.core.artefacts;

import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.core.accessors.MongoDBAccessorHelper;

import com.mongodb.MongoClient;



public class ArtefactAccessor {
			
	private MongoCollection artefacts;
		
	public ArtefactAccessor() {
		super();
	}

	public ArtefactAccessor(MongoClient client) {
		super();
		artefacts = MongoDBAccessorHelper.getCollection(client, "artefacts");
	}
	
	public <T extends AbstractArtefact> T  createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	public <T extends AbstractArtefact> T  createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		try {
			T artefact = artefactClass.newInstance();
			artefact.setName(name);
			if(copyChildren) {
				for(ObjectId childId:parentArtefact.getChildrenIDs()) {
					artefact.addChild(childId);
				}
			}
			return artefact;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public AbstractArtefact get(ObjectId artefactID) {
		AbstractArtefact artefact = artefacts.findOne(artefactID).as(AbstractArtefact.class);
		return artefact;
	}
	
	public void remove(ObjectId artefactID) {
		artefacts.remove(artefactID);
	}
	
	public AbstractArtefact get(String artefactID) {
		AbstractArtefact artefact = artefacts.findOne(new ObjectId(artefactID)).as(AbstractArtefact.class);
		return artefact;
	}
	
    public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
    	final Iterator<ObjectId> childrenIdIt = parent.getChildrenIDs()!=null?parent.getChildrenIDs().iterator():null;
    	Iterator<AbstractArtefact> childrenIt = new Iterator<AbstractArtefact>() {
			@Override
			public void remove() {}
			
			@Override
			public AbstractArtefact next() {
				return get(childrenIdIt.next());
			}
			
			@Override
			public boolean hasNext() {
				return childrenIdIt!=null?childrenIdIt.hasNext():false;
			}
		};
    	return childrenIt;
    }
	
	public AbstractArtefact save(AbstractArtefact artefact) {
		artefacts.save(artefact);
		return artefact;
	}
	
	public void save(List<? extends AbstractArtefact> artefacts) {
		this.artefacts.insert(artefacts.toArray());
	}
}
