package step.core.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.bson.Document;
import org.bson.conversions.Bson;

import step.core.GlobalContext;

public class FragmentSupplier {
	
	private List<String> excludedContexts;
	private List<Function<Session, Map<String, String>>> additionalQueryFragmentSuppliers;
	

	public FragmentSupplier(GlobalContext context) {
		additionalQueryFragmentSuppliers = new ArrayList<>();
		String excludedContextsValue = context.getConfiguration().getProperty("plugins.MultitenancyPlugin.excludedContexts", "");
		if(excludedContextsValue != null) {
			String[] split = excludedContextsValue.split(",");
			if(split.length > 0) {
				excludedContexts = Arrays.asList(split);
			}
		}
	}

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
		if(!excludedContexts.contains(collectionID)) {
			if(ignoreContext == null || !ignoreContext.equals("true")) {
				getAdditionalQueryFragmentSuppliers().forEach(s->{
					additionalQueryFragments.putAll(s.apply(session));
				});
			}
		}
		return additionalQueryFragments;	
	}
}
