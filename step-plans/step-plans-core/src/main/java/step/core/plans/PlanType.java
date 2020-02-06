package step.core.plans;

public interface PlanType<T extends Plan> {

	public Class<T> getPlanClass();
	
 	public PlanCompiler<T> getPlanCompiler();
	
 	public T newPlan(String template) throws Exception;
}
