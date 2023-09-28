package step.repositories.artifact;

import ch.exense.commons.io.FileHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallPlan;
import step.artefacts.TestSet;
import step.core.artefacts.AbstractArtefact;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.repositories.ImportResult;
import step.functions.Function;
import step.resources.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceArtifactRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(ResourceArtifactRepositoryTest.class);

    private ResourceArtifactRepository repo;
    private ResourceManagerImpl resourceManager;
    private InMemoryPlanAccessor planAccessor;

    @Before
    public void setup() throws IOException {
        this.resourceManager = new ResourceManagerImpl(FileHelper.createTempFolder(), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
        this.planAccessor = new InMemoryPlanAccessor();
        this.repo = new ResourceArtifactRepository(planAccessor, resourceManager);
    }

    @Test
    public void importArtefact() throws SimilarResourceExistingException {
        // In this test we have 2 plans and to keywords annotated with @Keyword
        // One of these plans also has the inner keyword declared in .plan file
        // We expect that the jar will be correctly parsed into the TestSet containing all @Keywords from the jar file (non-duplicated)
        // And also the test plan with inner keyword (planWithInnerKeyword.plan) should have this inner keyword (CompositeFunction) after parsing
        File file = new File("src/test/resources/step/repositories/artifact/plans-with-keywords.jar");
        try (InputStream is = new FileInputStream(file)) {
            ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
            Mockito.when(ctx.getObjectEnricher()).thenReturn(Mockito.mock(ObjectEnricher.class));
            Mockito.when(ctx.getExecutionId()).thenReturn("test-exec");

            // upload the jar
            Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, is, "plans-with-keywords.jar", false, null);
            log.info("Resource uploaded: {}", resource.getId().toString());

            Map<String, String> repoParams = new HashMap<>();
            repoParams.put(ResourceArtifactRepository.PARAM_RESOURCE_ID, resource.getId().toString());

            log.info("Importing...");
            ImportResult importResult = this.repo.importArtefact(ctx, repoParams);
            Assert.assertTrue(importResult.isSuccessful());
            Assert.assertNotNull(importResult.getPlanId());

            // get the imported TestSet
            Plan importedPlan = planAccessor.get(importResult.getPlanId());
            Assert.assertNotNull(importedPlan);
            Assert.assertTrue(importedPlan.getRoot() instanceof TestSet);

            // there are 2 annotated keywords in jar (keyword1 and keyword2)
            Collection<Function> functions = importedPlan.getFunctions();

            Assert.assertNotNull(functions.stream().filter(f -> f.getAttribute(Function.NAME).equals("keyword1")).findFirst().orElse(null));
            Assert.assertNotNull(functions.stream().filter(f -> f.getAttribute(Function.NAME).equals("keyword2")).findFirst().orElseThrow(null));
            Assert.assertEquals(2, functions.size());

            // find the plan having the inner keyword (planWithInnerKeyword.plan)
            List<CallPlan> callPlans = importedPlan.getRoot().getChildren().stream().map(p -> (CallPlan) p).collect(Collectors.toList());
            CallPlan callPlanWithInnerKeyword = callPlans.stream().filter(p -> p.getAttribute(AbstractArtefact.NAME).equals("planWithInnerKeyword.plan")).findFirst().orElse(null);
            Assert.assertNotNull(callPlanWithInnerKeyword);

            // load the plan
            Plan planWithInnerKeyword = planAccessor.get(callPlanWithInnerKeyword.getPlanId());

            // check the list of functions included in plan
            Collection<Function> planKeywords = planWithInnerKeyword.getFunctions();
            Assert.assertEquals(3, planKeywords.size());
            Assert.assertNotNull(planKeywords.stream().filter(f -> f.getAttribute(Function.NAME).equals("myInnerKeyword")).findFirst().orElse(null));
            Assert.assertNotNull(planKeywords.stream().filter(f -> f.getAttribute(Function.NAME).equals("keyword1")).findFirst().orElse(null));
            Assert.assertNotNull(planKeywords.stream().filter(f -> f.getAttribute(Function.NAME).equals("keyword2")).findFirst().orElse(null));

        } catch (IOException | InvalidResourceFormatException e) {
            throw new RuntimeException("Input stream exception", e);
        }

    }
}