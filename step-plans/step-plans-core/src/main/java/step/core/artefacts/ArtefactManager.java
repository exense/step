package step.core.artefacts;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

public class ArtefactManager {

	ArtefactAccessor accessor;
	
	public ArtefactManager(ArtefactAccessor accessor) {
		super();
		this.accessor = accessor;
	}

	public AbstractArtefact copyArtefact(String id) {
		return copyArtefact(id, null);
	}
	
	public AbstractArtefact copyArtefact(String id, String targetParentId) {
		ObjectId cloneId = copyRecursive(new ObjectId(id));
		
		AbstractArtefact target;
		if(targetParentId!=null) {
			target = accessor.get(targetParentId);
			target.addChild(cloneId);
			accessor.save(target);
		} else {
			target = accessor.get(cloneId);
			if(target.getAttributes()!=null) {
				String name = target.getAttributes().get("name");
				target.getAttributes().put("name", name+"_Copy");				
			}
			accessor.save(target);
		}
		return target;
	}
	
	private ObjectId copyRecursive(ObjectId id) {
		ObjectId cloneId = new ObjectId(); 
		AbstractArtefact artefact = accessor.get(id);
		artefact.setId(cloneId);	
		if(artefact.getChildrenIDs()!=null) {
			List<ObjectId> newChildren = new ArrayList<>();
			for(ObjectId childId:artefact.getChildrenIDs()) {
				newChildren.add(copyRecursive(childId));
			}
			artefact.setChildrenIDs(newChildren);
		}
		accessor.save(artefact);
		return cloneId;
	}
	
	public void removeRecursive(ObjectId artefactId) {
		AbstractArtefact artefact = accessor.get(artefactId);
		if(artefact!=null) {
			if(artefact.getChildrenIDs()!=null) {
				for(ObjectId childId:artefact.getChildrenIDs()) {
					removeRecursive(childId);
				}
			}
			accessor.remove(artefactId);
		}
	}
}
