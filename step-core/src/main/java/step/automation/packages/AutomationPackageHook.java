package step.automation.packages;

import step.core.objectenricher.EnricheableObject;

import java.util.List;
import java.util.Map;

public interface AutomationPackageHook<T> {

    default void onMainAutomationPackageManagerCreate(Map<String, Object> extensions){
    }

    default void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions){
    }

    default void onLocalAutomationPackageManagerCreate(Map<String, Object> extensions){
        onMainAutomationPackageManagerCreate(extensions);
    }

    /**
     * On reading the additional fields in yaml representation (additional data should be stored in AutomationPackageContent)
     */
    default void onAdditionalDataRead(String fieldName,
                                      List<?> yamlData,
                                      AutomationPackageContent targetContent) {
        // by default, just copy the yaml objects to automation package content
        targetContent.getAdditionalData().put(fieldName, yamlData);
    }

    /**
     * On preparing the staging to be later persisted in DB (objects should be added to targetStaging)
     */
    default void onPrepareStaging(String fieldName,
                                  AutomationPackageContext apContext,
                                  AutomationPackageContent apContent,
                                  List<?> objects,
                                  AutomationPackage oldPackage,
                                  AutomationPackageStaging targetStaging) {
        // by default, we simply put the objects to staging
        targetStaging.getAdditionalObjects().put(fieldName, (List<Object>) objects);
    }

    /**
     * Create the entities (taken from previously prepared staging) in database
     */
    default void onCreate(List<? extends T> entities, AutomationPackageContext context){
        for (T entity : entities) {
            if(entity instanceof EnricheableObject) {
                context.getEnricher().accept((EnricheableObject) entity);
            }
        }
    }

    /**
     * Delete the entities from database
     */
    default void onDelete(AutomationPackage automationPackage, AutomationPackageContext context) {
    }

}
