package step.repositories.artifact;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.TestRunStatus;
import step.core.repositories.TestSetStatusOverview;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class MavenArtifactRepositoryTest extends AbstractMavenArtifactRepositoryTest {

    @Override
    protected InMemoryControllerSettingAccessor setupControllerSettingsAccessor() {
        InMemoryControllerSettingAccessor controllerSettingAccessor = new InMemoryControllerSettingAccessor();
        controllerSettingAccessor.createSettingIfNotExisting("maven_settings_default", MAVEN_SETTINGS_NEXUS);
        return controllerSettingAccessor;
    }

    @Before
    public void before() {
        setup();
    }

    @After
    public void after() {
        cleanup();
    }

    @Test
    public void test() throws IOException {
        // getArtefactInfo
        ArtefactInfo artefactInfo = artifactRepository.getArtefactInfo(REPOSITORY_PARAMETERS);
        assertEquals("step-automation-packages-junit", artefactInfo.getName());
        assertEquals("TestSet", artefactInfo.getType());

        // getTestSetStatusOverview
        TestSetStatusOverview testSetStatusOverview = artifactRepository.getTestSetStatusOverview(REPOSITORY_PARAMETERS, null);
        List<String> expected = Stream.of(
                "plans/composite-simple-plan.yml", "plans/plan2.plan", "My custom keyword name",
                "explicitPlanWithExecutionParameter", "planWithAssert", "testAutomation.plan", "plans/plan3.plan",
                "Local Keyword", "plans/assertsTest.plan", "Inline Plan", "JMeter Plan", "Test Plan").sorted(String::compareTo).collect(Collectors.toList());
        assertEquals(expected,
                testSetStatusOverview.getRuns().stream().map(TestRunStatus::getTestplanName).sorted(String::compareTo).collect(Collectors.toList()));

        // importArtefact
        ImportResult tests = artifactRepository.importArtefact(executionContext, REPOSITORY_PARAMETERS);
        assertTrue(tests.isSuccessful());
        assertNotNull(tests.getPlanId());

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

}