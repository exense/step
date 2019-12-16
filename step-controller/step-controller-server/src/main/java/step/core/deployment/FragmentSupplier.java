package step.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;

import step.core.objectenricher.ObjectFilter;

public class FragmentSupplier {
	
	private final List<Function<Session, ObjectFilter>> additionalQueryFragmentSuppliers = new ArrayList<>();

	public ObjectFilter getObjectFilter(Session session) {
		List<ObjectFilter> objectFilters = additionalQueryFragmentSuppliers.stream().map(s->s.apply(session)).collect(Collectors.toList());
		return new ObjectFilter() {
			
			@Override
			public boolean test(Object t) {
				for (ObjectFilter objectFilter : objectFilters) {
					if(!objectFilter.test(t)) {
						return false;
					}
				}
				return true;
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				Map<String, String> result = new HashMap<>();
				objectFilters.forEach(f->result.putAll(f.getAdditionalAttributes()));
				return result;
			}
		};
	}
	
	private List<Bson> toBson(Map<String, String> additionalQueryFragmentSuppliers) {
		List<Bson> bson = new ArrayList<>();
		for( Entry<String, String> e : additionalQueryFragmentSuppliers.entrySet()) {
			bson.add(new Document("attributes."+e.getKey(), e.getValue()));
		}
		return bson;
	}

	public boolean add(Function<Session, ObjectFilter> e) {
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
			additionalQueryFragmentSuppliers.forEach(s->{
				additionalQueryFragments.putAll(s.apply(session).getAdditionalAttributes());
			});
		}
		return additionalQueryFragments;	
	}
}
