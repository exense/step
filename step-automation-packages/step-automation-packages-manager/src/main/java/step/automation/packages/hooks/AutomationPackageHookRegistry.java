package step.automation.packages.hooks;

import step.automation.packages.AutomationPackageReader;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.model.AutomationPackageContent;
import step.core.objectenricher.ObjectEnricher;

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
    public boolean onAdditionalDataRead(String fieldName, List<?> yamlData, AutomationPackageContent targetContent, AutomationPackageReader reader){
        AutomationPackageHook<?> hook = getHook(fieldName);
        if(hook != null){
            hook.onAdditionalDataRead(fieldName, yamlData, targetContent, reader);
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
                                        List<T> objects,
                                        AutomationPackage oldPackage,
                                        step.automation.packages.AutomationPackageManager.Staging targetStaging) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onPrepareStaging(fieldName, apContext, objects, oldPackage, targetStaging);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create the entities (taken from previously prepared staging) in database
     */
    public <T> boolean onCreate(String fieldName, List<T> objects, ObjectEnricher enricher) {
        AutomationPackageHook<T> hook = (AutomationPackageHook<T>) getHook(fieldName);
        if (hook != null) {
            hook.onCreate(objects, enricher);
            return true;
        } else {
            return false;
        }
    }

    public boolean isHookRegistered(String fieldName) {
        return getHook(fieldName) != null;
    }

    public void onAutomationPackageDelete(AutomationPackage automationPackage, AutomationPackageManager manager) {
        for (AutomationPackageHook<?> hook : registry.values()) {
            hook.onDelete(automationPackage, manager);
        }
    }

    public void register(String fieldName, AutomationPackageHook<?> automationPackageHook) {
        registry.put(fieldName, automationPackageHook);
    }
}
