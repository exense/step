package step.plugins.datatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;

import org.bson.conversions.Bson;

import step.core.deployment.AbstractServices;
import step.core.deployment.Session;

public abstract class AbstractTableService extends AbstractServices {

	protected TableManager tableManager;
	protected Collection<String> excludedContexts;

	public AbstractTableService() {
		super();
	}

	public void init() throws Exception {
		tableManager = getContext().get(TableManager.class);
		String excludedContextsValue = getContext().getConfiguration().getProperty("plugins.MultitenancyPlugin.excludedContexts", "");
		if(excludedContextsValue != null) {
			String[] split = excludedContextsValue.split(",");
			if(split.length > 0) {
				excludedContexts = Arrays.asList(split);
			}
		}
	}

	protected List<Bson> getAdditionalQueryFragmentsFromContext(ContainerRequestContext crc, String collectionID, String ignoreContext) {
		Session session = getSession(crc);
		List<Bson> additionalQueryFragments = new ArrayList<>();
		if(!excludedContexts.contains(collectionID)) {
			if(ignoreContext == null || !ignoreContext.equals("true")) {
				tableManager.additionalQueryFragmentSuppliers.forEach(s->{
					additionalQueryFragments.addAll(s.apply(session));
				});
			}
		}
		return additionalQueryFragments;
	}
}