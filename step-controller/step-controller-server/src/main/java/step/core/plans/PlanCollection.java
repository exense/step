package step.core.plans;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.core.accessors.Collection;

public class PlanCollection extends Collection {

	public PlanCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "plans");
	}

	@Override
	public List<Bson> getAdditionalQueryFragments() {
		ArrayList<Bson> fragments = new ArrayList<Bson>();
		fragments.add(new Document("visible", true));
		return fragments;
	}
}
