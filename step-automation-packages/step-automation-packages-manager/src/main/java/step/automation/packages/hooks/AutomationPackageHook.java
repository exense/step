package step.automation.packages.hooks;

import step.automation.packages.AutomationPackageReader;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.model.AutomationPackageContent;
import step.core.objectenricher.ObjectEnricher;

import java.util.List;

public interface AutomationPackageHook<T> {

    /**
     * On reading the additional fields in yaml representation (additional data should be stored in AutomationPackageContent)
     */
    default void onAdditionalDataRead(String fieldName,
                                      List<?> yamlData,
                                      AutomationPackageContent targetContent,
                                      AutomationPackageReader reader) {
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
                                  AutomationPackageManager.Staging targetStaging,
                                  AutomationPackageManager manager) {
        // by default, we simply put the objects to staging
        targetStaging.getAdditionalObjects().put(fieldName, (List<Object>) objects);
    }

    /**
     * Create the entities (taken from previously prepared staging) in database
     */
    default void onCreate(List<? extends T> entities, ObjectEnricher enricher, AutomationPackageManager manager){
    }

    /**
     * Delete the entities from database
     */
    default void onDelete(AutomationPackage automationPackage, AutomationPackageManager manager) {
    }

}
