package step.plugins.datatable;

import java.util.List;

import org.bson.conversions.Bson;

import step.core.deployment.FragmentSupplier;
import step.core.deployment.Session;

public class TableManager{

	private FragmentSupplier supplier;

	public TableManager(FragmentSupplier supplier) {
		this.supplier = supplier;
	}

	public List<Bson> getAdditionalQueryFragmentsFromContext(Session session, String collectionID, String ignoreContext) {
		return this.supplier.getAdditionalQueryFragmentsFromContextAsBson(session, collectionID, ignoreContext);
	}
}
