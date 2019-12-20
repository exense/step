package step.plugins.datatable;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;

import org.bson.conversions.Bson;

import step.core.deployment.AbstractServices;
import step.core.objectenricher.ObjectHookRegistry;

public abstract class AbstractTableService extends AbstractServices {

	protected TableManager tableManager;
	protected Collection<String> excludedContexts;

	public AbstractTableService() {
		super();
	}

	public void init() throws Exception {
		tableManager = new TableManager(getContext().get(ObjectHookRegistry.class));
	}

	protected List<Bson> getAdditionalQueryFragmentsFromContext(ContainerRequestContext crc, String collectionID, String ignoreContext) {
		return tableManager.getAdditionalQueryFragmentsFromContext(getSession(crc), collectionID, ignoreContext);
	}
}