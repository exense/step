package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.repositories.ImportResult;

import java.util.Map;

public class ArtifactRepositoryTest {

    @Test
    public void importArtefact() {
        InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
        InMemoryControllerSettingAccessor controllerSettingAccessor = new InMemoryControllerSettingAccessor();
        controllerSettingAccessor.createSettingIfNotExisting(ArtifactRepository.MAVEN_SETTINGS_XML, "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
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
                "</settings>");
        Configuration configuration = new Configuration();
        ArtifactRepository artifactRepository = new ArtifactRepository(planAccessor, controllerSettingAccessor, configuration);
        ExecutionContext executionContext = ExecutionEngine.builder().build().newExecutionContext();
        ImportResult tests = artifactRepository.importArtefact(executionContext, Map.of(ArtifactRepository.PARAM_GROUP_ID, "ch.exense.step",
                ArtifactRepository.PARAM_ARTEFACT_ID, "step-junit", ArtifactRepository.PARAM_VERSION, "0.0.0",
                ArtifactRepository.PARAM_CLASSIFIER, "tests"));
        Assert.assertTrue(tests.isSuccessful());
        Plan plan = planAccessor.findByAttributes(Map.of(AbstractOrganizableObject.NAME, "step-junit-0.0.0-tests.jar"));
        Assert.assertNotNull(plan);
    }
}