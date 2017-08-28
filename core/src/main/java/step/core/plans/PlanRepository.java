package step.core.plans;

import java.util.Map;

public interface PlanRepository {

	Plan load(Map<String, String> attributes);

	void save(Plan plan);

}