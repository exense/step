package step.core.export;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import step.attachments.FileResolver;
import step.automation.packages.*;
import step.core.Controller;
import step.core.entities.EntityManager;
import step.core.imports.ImportConfiguration;
import step.core.imports.ImportManager;
import step.core.imports.ImportResult;
import step.migration.MigrationManager;
import step.parameter.Parameter;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.parametermanager.ParameterManagerControllerPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static step.core.export.ExportManagerTest.*;

public class ImportExportAutomationPackageTest extends AbstractAutomationPackageManagerTest {

    private EntityManager entityManager;
    private MigrationManager migrationManager;

    @Before
    public void before() {
        super.before();
        entityManager = createEntityManager(getMockedEncryptionManager(), resourceManager, parameterAccessor, planAccessor
                ,functionAccessor, resourceManager.getResourceAccessor(), resourceManager.getResourceRevisionAccessor());

        migrationManager = createMigrationManager();
    }

    @Test
    public void testExportAndImportOfAp() throws Exception {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        SampleUploadingResult sampleUploadingResult;
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            AutomationPackageFileSource sample1FileSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);
            AutomationPackageUpdateParameter createParameters = new AutomationPackageUpdateParameterBuilder().forJunit()
                    .withAllowUpdate(false)
                    .withApSource(sample1FileSource)
                    .build();
            ObjectId id = manager.createOrUpdateAutomationPackage(createParameters).getId();
            assertNotNull(id);
        }

        long plansCount = planAccessor.stream().count();

        File testExportFile = new File("testExport.json");
        try (FileOutputStream outputStream = new FileOutputStream(testExportFile)) {
            ExportManager exportManager = new ExportManager(entityManager, resourceManager);
            Map<String, String> metadata = buildMetadata();
            List<String> additionalEntities = new ArrayList<>();
            additionalEntities.add(Parameter.ENTITY_NAME);

            ExportConfiguration exportConfig = new ExportConfiguration(outputStream, metadata, dummyObjectPredicate(), "plans", true, additionalEntities);
            ExportResult exportResult = exportManager.exportAll(exportConfig);

            assertEquals(1, exportResult.getMessages().size());
            assertEquals(ParameterManagerControllerPlugin.EXPORT_PROTECT_PARAM_WARN,exportResult.getMessages().toArray()[0]);


            //Reset all accessors
            before();
            ImportManager importManager = new ImportManager(entityManager, migrationManager, Controller.VERSION);
            ImportConfiguration importConfiguration = new ImportConfiguration(testExportFile, dummyObjectEnricher(), null, false);
            ImportResult importResult = importManager.importAll(importConfiguration);
            assertEquals(1,importResult.getMessages().size());
            assertEquals(ParameterManagerControllerPlugin.IMPORT_RESET_WARN,importResult.getMessages().toArray()[0]);

            assertEquals(plansCount, planAccessor.stream().count());
            assertTrue(planAccessor.stream().allMatch(p -> p.getCustomFields() == null || !p.getCustomFields().containsKey(AutomationPackageEntity.AUTOMATION_PACKAGE_ID)));
            assertTrue(functionAccessor.stream().allMatch(f -> f.getCustomFields() == null || !f.getCustomFields().containsKey(AutomationPackageEntity.AUTOMATION_PACKAGE_ID)));
            assertTrue(planAccessor.stream().allMatch(p -> p.getCustomFields() == null || !p.getCustomFields().containsKey(AutomationPackageEntity.AUTOMATION_PACKAGE_ID)));
            assertTrue(resourceManager.getResourceAccessor().stream().allMatch(p -> p.getCustomFields() == null || !p.getCustomFields().containsKey(AutomationPackageEntity.AUTOMATION_PACKAGE_ID)));
            assertTrue(resourceManager.getResourceRevisionAccessor().stream().allMatch(p -> p.getCustomFields() == null || !p.getCustomFields().containsKey(AutomationPackageEntity.AUTOMATION_PACKAGE_ID)));

            functionAccessor.stream().forEach(f -> {
                String automationPackageFile = f.getAutomationPackageFile();
                assertNotNull(automationPackageFile);
                assertTrue(FileResolver.isResourceRevision(automationPackageFile));
                if (f instanceof GeneralScriptFunction) {
                    GeneralScriptFunction gf = (GeneralScriptFunction) f;
                    String scriptFile = gf.getScriptFile().get();
                    assertNotNull(scriptFile);
                    assertTrue(FileResolver.isResourceRevision(scriptFile));
                }
            });
        } finally {
            testExportFile.delete();
        }
    }

}
