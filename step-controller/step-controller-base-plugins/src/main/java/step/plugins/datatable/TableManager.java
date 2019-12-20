package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.conversions.Bson;

import step.core.deployment.Session;
import step.core.objectenricher.ObjectHookRegistry;

public class TableManager{

	private ObjectHookRegistry objectHookRegistry;

	public TableManager(ObjectHookRegistry objectHookRegistry) {
		this.objectHookRegistry = objectHookRegistry;
	}

	public List<Bson> getAdditionalQueryFragmentsFromContext(Session session, String collectionID) {
		return getAdditionalQueryFragmentsFromContextAsBson(session, collectionID);
	}
	
	public List<Bson> getAdditionalQueryFragmentsFromContextAsBson(Session session, String collectionID){
		return toBson(objectHookRegistry.getObjectFilter(session).getAdditionalAttributes());
	}
	
	private List<Bson> toBson(Map<String, String> additionalQueryFragmentSuppliers) {
		List<Bson> bson = new ArrayList<>();
		for( Entry<String, String> e : additionalQueryFragmentSuppliers.entrySet()) {
			bson.add(new Document("attributes."+e.getKey(), e.getValue()));
		}
		return bson;
	}
}
