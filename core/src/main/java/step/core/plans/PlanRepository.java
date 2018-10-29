package step.core.plans;

import java.util.Map;

public interface PlanRepository {

	/**
	 * Load a Plan by attributes
	 * 
	 * @param attributes the attributes (key-value pairs) of the Plan to be loaded.  
	 * @return the first Plan matching the attributes
	 */
	Plan load(Map<String, String> attributes);

	/**
	 * Save (Insert or Update) a Plan
	 * @param plan
	 */
	void save(Plan plan);

}