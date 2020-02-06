package step.core.plans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlanTypeRegistry {

	private final Map<Class<? extends Plan>, PlanType<?>> registry = new ConcurrentHashMap<>();
	
	public void register(PlanType<?> planType) {
		registry.put(planType.getPlanClass(), planType);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Plan> PlanType<T> getPlanType(Class<T> planClass) {
		PlanType<?> planType = registry.get(planClass);
		return (PlanType<T>) planType;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Plan> PlanType<T> getPlanType(String planTypeName) {
		PlanType<?> planType = registry.values().stream().filter(p->{
			return p.getPlanClass().getName().equals(planTypeName);
		}).findFirst().get();
		return (PlanType<T>) planType;
	}
}
