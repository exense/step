package step.migration.tasks;

import org.junit.Test;
import step.automation.packages.AutomationPackage;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationContext;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class V29_0_UpdateAutomationPackageModelTest {

    @Test
    public void testV29_0_UpdateAutomationPackageModel() {
        InMemoryCollectionFactory inMemoryCollectionFactory = new InMemoryCollectionFactory(new Properties());
        Collection<Document> automationPackages = inMemoryCollectionFactory.getCollection("automationPackages", Document.class);
        Document documentWithVersion = new Document();
        documentWithVersion.put("version","1.0.0");
        automationPackages.save(documentWithVersion);
        automationPackages.save(new Document());
        V29_0_UpdateAutomationPackageModel v290UpdateAutomationPackageModel = new V29_0_UpdateAutomationPackageModel(inMemoryCollectionFactory, new MigrationContext());
        v290UpdateAutomationPackageModel.runUpgradeScript();
        assertEquals(2, v290UpdateAutomationPackageModel.successCount.get());
        Collection<AutomationPackage> automationPackages1 = inMemoryCollectionFactory.getCollection("automationPackages", AutomationPackage.class);
        List<AutomationPackage> collect = automationPackages1.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        assertEquals(2, collect.size());
        AutomationPackage automationPackage1 = collect.get(0);
        AutomationPackage automationPackage2 = collect.get(1);
        if ("1.0.0".equals(automationPackage1.getVersionName())) {
            assertNull(automationPackage2.getVersionName());
        } else if ("1.0.0".equals(automationPackage2.getVersionName())) {
            assertNull(automationPackage1.getVersionName());
        } else {
            fail("unexpected values of versionName after migration");
        }
    }

}