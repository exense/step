package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.InMemoryPlanAccessor;
import step.core.repositories.ImportResult;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.type.FunctionTypeRegistry;
import step.resources.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ResourceArtifactRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(ResourceArtifactRepositoryTest.class);

    private ResourceArtifactRepository repo;
    private ResourceManagerImpl resourceManager;
    private AutomationPackageManager apManager;
    private FunctionTypeRegistry functionTypeRegistry;

    @Before
    public void setup() throws IOException {
        this.resourceManager = new ResourceManagerImpl(FileHelper.createTempFolder(), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());

        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageReader apReader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, new AutomationPackageSerializationRegistry(), new Configuration());
        this.functionTypeRegistry = MavenArtifactRepositoryTest.prepareTestFunctionTypeRegistry();
        InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
        this.apManager = AutomationPackageManager.createLocalAutomationPackageManager(functionTypeRegistry, functionAccessor, new InMemoryPlanAccessor(), new LocalResourceManagerImpl(), apReader, hookRegistry);
        this.repo = new ResourceArtifactRepository(resourceManager, apManager, functionTypeRegistry, functionAccessor);
    }

    @Test
    public void importArtefact() throws SimilarResourceExistingException {
        // In this test we have 2 plans and to keywords annotated with @Keyword
        // One of these plans also has the inner keyword declared in .plan file
        // We expect that the jar will be correctly parsed into the TestSet containing all @Keywords from the jar file (non-duplicated)
        // And also the test plan with inner keyword (planWithInnerKeyword.plan) should have this inner keyword (CompositeFunction) after parsing
        File file = new File("src/test/resources/step/repositories/artifact/plans-with-keywords.jar");
        try (InputStream is = new FileInputStream(file)) {
            // mock the context, which is normally prepared via FunctionPlugin
            ExecutionContext executionContext = ExecutionEngine.builder().build().newExecutionContext();
            executionContext.put(FunctionAccessor.class, new InMemoryFunctionAccessorImpl());
            executionContext.put(FunctionTypeRegistry.class, functionTypeRegistry);

            // upload the jar
            Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, is, "plans-with-keywords.jar", false, null);
            log.info("Resource uploaded: {}", resource.getId().toString());

            Map<String, String> repoParams = new HashMap<>();
            repoParams.put(ResourceArtifactRepository.PARAM_RESOURCE_ID, resource.getId().toString());

            log.info("Importing...");
            ImportResult importResult = this.repo.importArtefact(executionContext, repoParams);
            Assert.assertTrue(importResult.isSuccessful());
            Assert.assertNotNull(importResult.getPlanId());

        } catch (IOException | InvalidResourceFormatException e) {
            throw new RuntimeException("Input stream exception", e);
        }

    }
}