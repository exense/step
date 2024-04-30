package step.automation.packages.hooks;

import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.model.AutomationPackageContent;
import step.core.objectenricher.ObjectEnricher;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutomationPackageHookRegistry {

    private final Map<String, AutomationPackageHook<?>> registry = new ConcurrentHashMap<>();

    public AutomationPackageHookRegistry() {
    }

    public AutomationPackageHook<?> getHook(String fieldName) {
        return registry.get(fieldName);
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
                                        AutomationPackageManager.Staging targetStaging,
                                        AutomationPackageManager manager) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onPrepareStaging(fieldName, apContext, apContent, objects, oldPackage, targetStaging, manager);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create the entities (taken from previously prepared staging) in database
     */
    public <T> boolean onCreate(String fieldName, List<T> objects, ObjectEnricher enricher, AutomationPackageManager manager) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onCreate(objects, enricher, manager);
            return true;
        } else {
            return false;
        }
    }

    public boolean isHookRegistered(String fieldName) {
        return getHook(fieldName) != null;
    }

    public void onAutomationPackageDelete(AutomationPackage automationPackage, AutomationPackageManager manager, Collection<String> excludedHookNames) {
        for (Map.Entry<String, AutomationPackageHook<?>> hook : registry.entrySet()) {
            if (excludedHookNames == null || !excludedHookNames.contains(hook.getKey())) {
                hook.getValue().onDelete(automationPackage, manager);
            }
        }
    }

    public void register(String fieldName, AutomationPackageHook<?> automationPackageHook) {
        registry.put(fieldName, automationPackageHook);
    }

    public Map<String, AutomationPackageHook<?>> unmodifiableRegistry(){
        return Collections.unmodifiableMap(registry);
    }
}
