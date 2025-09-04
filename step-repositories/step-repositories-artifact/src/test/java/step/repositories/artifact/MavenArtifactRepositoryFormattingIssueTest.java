package step.repositories.artifact;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.resources.LocalResourceManagerImpl;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenArtifactRepositoryFormattingIssueTest extends AbstractMavenArtifactRepositoryTest {

    protected LocalResourceManagerImpl resourceManager;

    protected InMemoryControllerSettingAccessor setupControllerSettingsAccessor() {
        InMemoryControllerSettingAccessor controllerSettingAccessor = new InMemoryControllerSettingAccessor();
        controllerSettingAccessor.createSettingIfNotExisting("maven_settings_default", "settings> </settings>");
        return controllerSettingAccessor;
    }

    @Before
    public void before(){
        this.resourceManager = new LocalResourceManagerImpl();
        setup();
    }

    @After
    public void after() {
       cleanup();
    }

    @Test
    public void testFormattingIssueInMavenSettings() throws IOException {
        // getArtefactInfo
        ArtefactInfo artefactInfo = artifactRepository.getArtefactInfo(REPOSITORY_PARAMETERS);
        assertEquals("step-automation-packages-junit", artefactInfo.getName());
        assertEquals("TestSet", artefactInfo.getType());

        // getTestSetStatusOverview
        Exception actualException = null;
        try {
            artifactRepository.getTestSetStatusOverview(REPOSITORY_PARAMETERS, null, "testUser");
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