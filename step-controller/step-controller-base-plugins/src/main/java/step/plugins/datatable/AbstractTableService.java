package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;

import org.bson.conversions.Bson;

import step.core.deployment.AbstractServices;
import step.core.deployment.Session;

public abstract class AbstractTableService extends AbstractServices {

	protected TableManager tableManager;
	
	public AbstractTableService() {
		super();
	}

	public void init() throws Exception {
		tableManager = getContext().get(TableManager.class);
	}
	
	protected List<Bson> getAdditionalQueryFragmentsFromContext(ContainerRequestContext crc) {
		Session session = getSession(crc);
		List<Bson> additionalQueryFragments = new ArrayList<>();
		tableManager.additionalQueryFragmentSuppliers.forEach(s->{
			additionalQueryFragments.addAll(s.apply(session));
		});
		return additionalQueryFragments;
	}
}