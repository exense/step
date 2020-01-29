package step.core.plans;

public interface PlanCompiler<T extends Plan> {

	public T compile(T plan) throws Exception;
}
