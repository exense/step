package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bson.conversions.Bson;

import step.core.deployment.Session;

public class TableManager {

	protected List<Function<Session, List<Bson>>> additionalQueryFragmentSuppliers = new ArrayList<>();

	public List<Function<Session, List<Bson>>> getAdditionalQueryFragmentSuppliers() {
		return additionalQueryFragmentSuppliers;
	}

	public void setAdditionalQueryFragmentSuppliers(List<Function<Session, List<Bson>>> additionalQueryFragmentSuppliers) {
		this.additionalQueryFragmentSuppliers = additionalQueryFragmentSuppliers;
	}

	public boolean add(Function<Session, List<Bson>> e) {
		return additionalQueryFragmentSuppliers.add(e);
	}
	
}
