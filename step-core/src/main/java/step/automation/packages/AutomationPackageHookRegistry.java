package step.automation.packages;

import step.core.AbstractStepContext;
import step.core.repositories.ImportResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutomationPackageHookRegistry {

    // Attention: This is a LinkedHashMap because order is important, hooks must be processed in the correct order
    private final LinkedHashMap<String, AutomationPackageHook<?>> registry = new LinkedHashMap<>();

    public AutomationPackageHookRegistry() {
    }

    public AutomationPackageHook<?> getHook(String fieldName) {
        return registry.get(fieldName);
    }

    public List<String> getOrderedHookFieldNames() {
        return new ArrayList<>(registry.keySet());
    }

    /**
     * On reading the additional fields in yaml representation (additional data should be stored in AutomationPackageContent)
     */
    public boolean onAdditionalDataRead(String fieldName, List<?> yamlData, AutomationPackageContent targetContent){
        AutomationPackageHook<?> hook = getHook(fieldName);
        if(hook != null){
            hook.onAdditionalDataRead(fieldName, yamlData, targetContent);
            return true;
        } else {
            return false;
        }
    }

    /**
     * On preparing the staging to be later persisted in DB (objects should be added to targetStaging)
     */
    public <T> boolean onPrepareStaging(String fieldName,
                                        AutomationPackageContext apContext,
                                        AutomationPackageContent apContent,
                                        List<T> objects,
                                        AutomationPackage oldPackage,
                                        AutomationPackageStaging targetStaging) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onPrepareStaging(fieldName, apContext, apContent, objects, oldPackage, targetStaging);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create the entities (taken from previously prepared staging) in database
     */
    public <T> boolean onCreate(String fieldName, List<T> objects, AutomationPackageContext context) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onCreate(objects, context);
            return true;
        } else {
            return false;
        }
    }

    public boolean isHookRegistered(String fieldName) {
        return getHook(fieldName) != null;
    }

    public void onAutomationPackageDelete(AutomationPackage automationPackage, AutomationPackageContext context, Collection<String> excludedHookNames) {
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            if (excludedHookNames == null || !excludedHookNames.contains(hook.getKey())) {
                hook.getValue().onDelete(automationPackage, context);
            }
        }
    }

    public void onMainAutomationPackageManagerCreate(Map<String, Object> extensions) {
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            hook.getValue().onMainAutomationPackageManagerCreate(extensions);
        }
    }

    public void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions) {
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            hook.getValue().onIsolatedAutomationPackageManagerCreate(extensions);
        }
    }

    public void onLocalAutomationPackageManagerCreate(Map<String, Object> extensions) {
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            hook.getValue().onLocalAutomationPackageManagerCreate(extensions);
        }
    }

    public void beforeIsolatedExecution(AutomationPackage automationPackage, AbstractStepContext executionContext, Map<String, Object> apManagerExtensions, ImportResult importResult){
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            hook.getValue().beforeIsolatedExecution(automationPackage, executionContext, apManagerExtensions, importResult);
        }
    }

    public void register(String fieldName, AutomationPackageHook<?> automationPackageHook) {
        registry.put(fieldName, automationPackageHook);
    }

    public Map<String, AutomationPackageHook<?>> unmodifiableRegistry(){
        return Collections.unmodifiableMap(registry);
    }
}
