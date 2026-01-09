package step.automation.packages;

import step.core.objectenricher.ObjectEnricher;
import step.resources.ResourceManager;

import java.util.Map;

public class StagingAutomationPackageContext extends AutomationPackageContext {

    private final AutomationPackageArchive automationPackageArchive;

    public StagingAutomationPackageContext(AutomationPackage automationPackage, AutomationPackageOperationMode operationMode,
                                           ResourceManager resourceManager, AutomationPackageArchive automationPackageArchive,
                                           AutomationPackageContent packageContent, String actorUser, ObjectEnricher enricher, Map<String, Object> extensions) {
        super(automationPackage, operationMode, resourceManager, packageContent, actorUser, enricher, extensions);
        this.automationPackageArchive = automationPackageArchive;
    }

    public AutomationPackageArchive getAutomationPackageArchive() {
        return automationPackageArchive;
    }
}
