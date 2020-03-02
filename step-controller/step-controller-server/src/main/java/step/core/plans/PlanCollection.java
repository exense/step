package step.core.plans;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.core.accessors.collections.Collection;

public class PlanCollection extends Collection<Plan> {

	public PlanCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "plans", Plan.class, true);
	}

	@Override
	public List<Bson> getAdditionalQueryFragments(JsonObject queryParameters) {
		ArrayList<Bson> fragments = new ArrayList<Bson>();
		fragments.add(new Document("visible", true));
		return fragments;
	}
}
