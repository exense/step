package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.TestSetStatusOverview;
import step.resources.LocalResourceManagerImpl;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MavenArtifactRepositoryTest {

    private static final Map<String, String> REPOSITORY_PARAMETERS = Map.of(MavenArtifactRepository.PARAM_GROUP_ID, "ch.exense.step",
            MavenArtifactRepository.PARAM_ARTIFACT_ID, "step-automation-packages-junit", MavenArtifactRepository.PARAM_VERSION, "0.0.0",
            MavenArtifactRepository.PARAM_CLASSIFIER, "tests");
    private static final String MAVEN_SETTINGS_NEXUS = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
            "    <profiles>\n" +
            "        <profile>\n" +
            "          <id>default</id>\n" +
            "          <repositories>\n" +
            "            <repository>\n" +
            "              <id>exense</id>\n" +
            "              <name>Exense</name>\n" +
            "              <url>https://nexus-enterprise.exense.ch/repository/container-dependency/</url>\n" +
            "            </repository>\n" +
            "          </repositories>\n" +
            "        </profile>\n" +
            "    </profiles>\n" +
            "    <activeProfiles>\n" +
            "        <activeProfile>default</activeProfile>\n" +
            "    </activeProfiles>\n" +
            "</settings>";
    private MavenArtifactRepository artifactRepository;
    private ExecutionContext executionContext;
    private InMemoryPlanAccessor planAccessor;
    private LocalResourceManagerImpl resourceManager;

    @Before
    public void before() {
        planAccessor = new InMemoryPlanAccessor();
        resourceManager = new LocalResourceManagerImpl();
        InMemoryControllerSettingAccessor controllerSettingAccessor = new InMemoryControllerSettingAccessor();
        controllerSettingAccessor.createSettingIfNotExisting("maven_settings_default", MAVEN_SETTINGS_NEXUS);

        Configuration configuration = new Configuration();
        artifactRepository = new MavenArtifactRepository(planAccessor, resourceManager, controllerSettingAccessor, configuration, null);
        executionContext = ExecutionEngine.builder().build().newExecutionContext();
    }

    @Test
    public void test() {
        // getArtefactInfo
        ArtefactInfo artefactInfo = artifactRepository.getArtefactInfo(REPOSITORY_PARAMETERS);
        assertEquals("step-automation-packages-junit", artefactInfo.getName());
        assertEquals("TestSet", artefactInfo.getType());

        // getTestSetStatusOverview
        TestSetStatusOverview testSetStatusOverview = artifactRepository.getTestSetStatusOverview(REPOSITORY_PARAMETERS, null);
        assertEquals(Set.of("plans/composite-simple-plan.yml", "plans/plan2.plan", "My custom keyword name", "explicitPlanWithExecutionParameter", "planWithAssert", "testAutomation.plan", "plans/plan3.plan", "Local Keyword", "plans/assertsTest.plan", "Inline Plan"),
                testSetStatusOverview.getRuns().stream().map(r -> r.getTestplanName()).collect(Collectors.toSet()));

        // importArtefact
        ImportResult tests = artifactRepository.importArtefact(executionContext, REPOSITORY_PARAMETERS);
        assertTrue(tests.isSuccessful());
        Plan plan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, "step-automation-packages-junit-0.0.0-tests.jar"));
        assertNotNull(plan);

        artifactRepository.exportExecution(executionContext, REPOSITORY_PARAMETERS);
    }

    @Test
    public void testMissingParameter() {
        Exception actualException = null;

        // getArtefactInfo
        try {
           artifactRepository.getArtefactInfo(Map.of());
        } catch (Exception e) {
            actualException = e;
        }
        assertEquals("Missing required parameter artifactId", actualException.getMessage());

        // getTestSetStatusOverview
        try {
            artifactRepository.getTestSetStatusOverview(Map.of(), null);
        } catch (Exception e) {
            actualException = e;
        }
        assertEquals("Missing required parameter artifactId", actualException.getMessage());

        try {
            artifactRepository.importArtefact(executionContext, Map.of());
        } catch (Exception e) {
            actualException = e;
        }
        assertEquals("Missing required parameter artifactId", actualException.getMessage());
    }

    @Test
    public void testFormattingIssueInMavenSettings() {
        InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
        InMemoryControllerSettingAccessor controllerSettingAccessor = new InMemoryControllerSettingAccessor();
        controllerSettingAccessor.createSettingIfNotExisting("maven_settings_default", "settings> </settings>");
        Configuration configuration = new Configuration();
        MavenArtifactRepository artifactRepository = new MavenArtifactRepository(planAccessor, resourceManager, controllerSettingAccessor, configuration, null);
        ExecutionContext executionContext = ExecutionEngine.builder().build().newExecutionContext();

        // getArtefactInfo
        ArtefactInfo artefactInfo = artifactRepository.getArtefactInfo(REPOSITORY_PARAMETERS);
        assertEquals("step-automation-packages-junit", artefactInfo.getName());
        assertEquals("TestSet", artefactInfo.getType());

        // getTestSetStatusOverview
        Exception actualException = null;
        try {
            artifactRepository.getTestSetStatusOverview(REPOSITORY_PARAMETERS, null);
        } catch (Exception e) {
            actualException = e;
        }
        assertTrue(actualException.getMessage().contains("Non-parseable settings (memory)"));

        // importArtefact
        ImportResult tests = artifactRepository.importArtefact(executionContext, REPOSITORY_PARAMETERS);
        Assert.assertFalse(tests.isSuccessful());
        assertTrue(tests.getErrors().get(0).contains("Non-parseable settings (memory)"));
    }
}