package step.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.bson.Document;
import org.bson.conversions.Bson;

public class FragmentSupplier {
	
	private final List<Function<Session, Map<String, String>>> additionalQueryFragmentSuppliers = new ArrayList<>();

	public List<Function<Session, Map<String, String>>> getAdditionalQueryFragmentSuppliers() {
		return additionalQueryFragmentSuppliers;
	}

	private List<Bson> toBson(Map<String, String> additionalQueryFragmentSuppliers) {
		List<Bson> bson = new ArrayList<>();
		for( Entry<String, String> e : additionalQueryFragmentSuppliers.entrySet()) {
			bson.add(new Document("attributes."+e.getKey(), e.getValue()));
		}
		return bson;
	}

	public boolean add(Function<Session, Map<String, String>> e) {
		return additionalQueryFragmentSuppliers.add(e);
	}
	
	public Map<String, String> getAdditionalQueryFragmentsFromContextAsAttributes(Session session, String collectionID, String ignoreContext){
		return computeFragments(session, collectionID, ignoreContext);
	}
	
	public List<Bson> getAdditionalQueryFragmentsFromContextAsBson(Session session, String collectionID, String ignoreContext){
		return toBson(computeFragments(session, collectionID, ignoreContext));
	}
	
	private Map<String, String> computeFragments(Session session, String collectionID, String ignoreContext) {
		Map<String, String> additionalQueryFragments = new HashMap<String, String>();
		if(ignoreContext == null || !ignoreContext.equals("true")) {
			getAdditionalQueryFragmentSuppliers().forEach(s->{
				additionalQueryFragments.putAll(s.apply(session));
			});
		}
		return additionalQueryFragments;	
	}
}
