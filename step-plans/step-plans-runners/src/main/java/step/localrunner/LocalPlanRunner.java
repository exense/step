package step.localrunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import step.core.plans.runner.DefaultPlanRunner;
import step.engine.PlanRunnerImpl;

/**
 * A runner that runs plans and functions locally.
 * The list of classes containing functions has to be passed to the constructor
 * 
 * @deprecated Use {@link PlanRunnerImpl} instead
 * @author Jérôme Comte
 *
 */
public class LocalPlanRunner extends DefaultPlanRunner {
	
	protected List<Class<?>> functionClasses;
	protected Map<String, String> properties;
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Class<?>... functionClasses) {
		this(null, Arrays.asList(functionClasses));
	}
	
	/**
	 * @param functionClasses functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(List<Class<?>> functionClasses) {
		this(null, functionClasses);
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, Class<?>... functionClasses) {
		this(properties, Arrays.asList(functionClasses));
	}
	
	/**
	 * @param properties a map containing the properties that are usually set under Parameters in the UI
	 * @param functionClasses the list of Classes containing the functions (aka Keywords)
	 */
	public LocalPlanRunner(Map<String, String> properties, List<Class<?>> functionClasses) {
		super();
		this.properties = properties;
		this.functionClasses = functionClasses;
	}
}
