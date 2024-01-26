package step.automation.packages.hooks;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.scheduler.ExecutiontTaskParameters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutomationPackageHookRegistry {

    private Map<Class<? extends AbstractIdentifiableObject>, AutomationPackageHook> registry = new ConcurrentHashMap<>();

    public AutomationPackageHook getHook(Class<? extends AbstractIdentifiableObject> clazz) {
        return registry.get(clazz);
    }

    public boolean onCreate(ExecutiontTaskParameters execTasksParameter) {
        AutomationPackageHook hook = getHook(execTasksParameter.getClass());
        if (hook != null) {
            hook.onCreate(execTasksParameter);
            return true;
        } else {
            return false;
        }
    }

    public boolean onDelete(ExecutiontTaskParameters execTasksParameter) {
        AutomationPackageHook hook = getHook(execTasksParameter.getClass());
        if (hook != null) {
            hook.onDelete(execTasksParameter);
            return true;
        } else {
            return false;
        }
    }

    public void register(Class<? extends AbstractIdentifiableObject> entityClass, AutomationPackageHook automationPackageHook) {
        registry.put(entityClass, automationPackageHook);
    }
}
