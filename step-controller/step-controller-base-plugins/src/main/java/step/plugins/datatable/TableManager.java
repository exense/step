package step.plugins.datatable;

import java.util.ArrayList;
import java.util.HashMap;
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

	public List<Bson> getAdditionalQueryFragmentsFromContext(Session session, String collectionID, String ignoreContext) {
		return getAdditionalQueryFragmentsFromContextAsBson(session, collectionID, ignoreContext);
	}
	
	public List<Bson> getAdditionalQueryFragmentsFromContextAsBson(Session session, String collectionID, String ignoreContext){
		return toBson(computeFragments(session, collectionID, ignoreContext));
	}
	
	private List<Bson> toBson(Map<String, String> additionalQueryFragmentSuppliers) {
		List<Bson> bson = new ArrayList<>();
		for( Entry<String, String> e : additionalQueryFragmentSuppliers.entrySet()) {
			bson.add(new Document("attributes."+e.getKey(), e.getValue()));
		}
		return bson;
	}
	
	private Map<String, String> computeFragments(Session session, String collectionID, String ignoreContext) {
		Map<String, String> additionalQueryFragments = new HashMap<String, String>();
		if(ignoreContext == null || !ignoreContext.equals("true")) {
			Map<String, String> additionalAttributes = objectHookRegistry.getObjectFilter(session).getAdditionalAttributes();
			additionalQueryFragments.putAll(additionalAttributes);
		}
		return additionalQueryFragments;	
	}
}
