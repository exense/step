package step.automation.packages;

import org.bson.types.ObjectId;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;
import step.core.repositories.ImportResult;

import java.util.ArrayList;
import java.util.HashMap;
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
        if (yamlData != null) {
            List<Object> existingData = (List<Object>) targetContent.getAdditionalData().get(fieldName);
            if (existingData != null) {
                existingData.addAll(yamlData);
            } else {
                targetContent.getAdditionalData().put(fieldName, new ArrayList<>(yamlData));
            }
        }
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
        if (objects != null) {
            List<Object> existingData = (List<Object>) targetStaging.getAdditionalObjects().get(fieldName);
            if (existingData != null) {
                existingData.addAll(objects);
            } else {
                targetStaging.getAdditionalObjects().put(fieldName, new ArrayList<>(objects));
            }
        }

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

    /**
     * Prepare execution context before isolated execution
     */
    default void beforeIsolatedExecution(AutomationPackage automationPackage, AbstractStepContext executionContext, Map<String, Object> apManagerExtensions, ImportResult importResult){

    }

    /**
     * Returns the map of database entities managed by this hook
     */
    default Map<String, List<? extends AbstractOrganizableObject>> getEntitiesForAutomationPackage(ObjectId automationPackageId, AutomationPackageContext automationPackageContext){
        return new HashMap<>();
    }
}
