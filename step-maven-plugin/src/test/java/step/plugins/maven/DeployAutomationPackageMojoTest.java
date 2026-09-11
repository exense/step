/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import step.cli.DeployAutomationPackageTool;
import step.cli.parameters.ApDeployParameters;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeployAutomationPackageMojoTest extends AbstractMojoTest {

    @Test
    public void testUpload() throws Exception {
        DeployAutomationPackageMojoTestable mojo = new DeployAutomationPackageMojoTestable();

        // configure mojo with test parameters and mocked Maven Project
        configureMojo(mojo);
        mojo.execute();

        Mockito.verify(mojo.mockedTool, Mockito.times(1)).execute();
        Assert.assertEquals("http://localhost:8080", mojo.toolUrl);
        Assert.assertEquals(false, mojo.toolAsync);
        Assert.assertEquals(TENANT_1.getName(), mojo.toolProjectName);
        Assert.assertEquals("dummyToken", mojo.toolAuthToken);

        // nothing configured: the package is deployed without any attribute nor routing criteria
        Assert.assertNull(mojo.deployParameters.getPlansAttributes());
        Assert.assertNull(mojo.deployParameters.getKeywordsAttributes());
        Assert.assertNull(mojo.deployParameters.getTokenSelectionCriteria());
        Assert.assertNull(mojo.deployParameters.getExecuteKeywordsOnController());
    }

    /**
     * The deployment configurations (attributes and routing options) can be configured in the pom.xml and overridden
     * one by one with system properties, as the execution parameters of the execute mojo already are.
     */
    @Test
    public void testUploadWithDeploymentConfigurations() throws Exception {
        DeployAutomationPackageMojoTestable mojo = new DeployAutomationPackageMojoTestable();

        configureMojo(mojo);
        Map<String, String> plansAttributes = new HashMap<>();
        plansAttributes.put("planKey1", "xmlPlanValue1");
        plansAttributes.put("planKey2", "xmlPlanValue2");
        mojo.setPlansAttributes(plansAttributes);
        mojo.setPlansAttributesRaw("planKey1=rawPlanValue1;planKey3=rawPlanValue3");
        mojo.setKeywordsAttributes(Map.of("keywordKey1", "xmlKeywordValue1"));
        mojo.setTokenSelectionCriteriaRaw("os=linux;team=core");
        mojo.setExecuteKeywordsOnController(true);
        mojo.execute();

        ApDeployParameters params = mojo.deployParameters;
        // the values of the system property take precedence over the ones of the pom.xml
        Assert.assertEquals(Map.of("planKey1", "rawPlanValue1", "planKey2", "xmlPlanValue2", "planKey3", "rawPlanValue3"),
            params.getPlansAttributes());
        Assert.assertEquals(Map.of("keywordKey1", "xmlKeywordValue1"), params.getKeywordsAttributes());
        Assert.assertEquals(Map.of("os", "linux", "team", "core"), params.getTokenSelectionCriteria());
        Assert.assertEquals(Boolean.TRUE, params.getExecuteKeywordsOnController());
    }

    @Test
    public void testUploadWithMalformedDeploymentConfiguration() throws Exception {
        DeployAutomationPackageMojoTestable mojo = new DeployAutomationPackageMojoTestable();

        configureMojo(mojo);
        mojo.setTokenSelectionCriteriaRaw("criterionWithoutValue");

        // as for a malformed execution parameter of the execute mojo, the build fails with the reason as cause
        MojoExecutionException e = Assert.assertThrows(MojoExecutionException.class, mojo::execute);
        Assert.assertEquals("Invalid token selection criterion format 'criterionWithoutValue', expected 'key=value'. " +
            "Multiple parameters should be separated by a semicolon ';' (ex: key1=value1;key2=value2).", e.getCause().getMessage());
    }

    private void configureMojo(DeployAutomationPackageMojoTestable mojo) throws URISyntaxException {
        mojo.setUrl("http://localhost:8080");
        mojo.setBuildFinalName("Test build name");
        mojo.setProjectVersion(VERSION_ID);
        mojo.setArtifactClassifier("jar-with-dependencies");
        mojo.setStepProjectName(TENANT_1.getName());
        mojo.setAuthToken("dummyToken");
        mojo.setAsync(false);
        mojo.setForceRefreshOfSnapshots(true);

        MavenProject mockedProject = Mockito.mock(MavenProject.class);
        Artifact mainArtifact = createArtifactMock();

        Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);
        Mockito.when(mockedProject.getArtifactId()).thenReturn(ARTIFACT_ID);
        Mockito.when(mockedProject.getGroupId()).thenReturn(GROUP_ID);
        Mockito.when(mockedProject.getVersion()).thenReturn(VERSION_ID);

        Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();
        Mockito.when(mockedProject.getArtifacts()).thenReturn(Set.of(mainArtifact, jarWithDependenciesArtifact));
        Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(mainArtifact, jarWithDependenciesArtifact));
        mojo.setProject(mockedProject);
    }

    private static class DeployAutomationPackageMojoTestable extends DeployAutomationPackageMojo {

        private final DeployAutomationPackageTool mockedTool = Mockito.mock(DeployAutomationPackageTool.class);

        private String toolUrl;
        private String toolProjectName;
        private String toolAuthToken;
        private Boolean toolAsync;
        private Boolean toolforceRefreshOfSnapshots;
        private ApDeployParameters deployParameters;

        public DeployAutomationPackageMojoTestable() {
        }

        @Override
        protected DeployAutomationPackageTool createTool(String url, String projectName, String authToken, Boolean async, String apVersion, String activationExpr, Boolean forceRefreshOfSnapshots) throws MojoExecutionException {
            this.toolAsync = async;
            this.toolUrl = url;
            this.toolProjectName = projectName;
            this.toolAuthToken = authToken;
            this.toolforceRefreshOfSnapshots = forceRefreshOfSnapshots;
            // the real parameters are built, so that what the tool would receive can be asserted
            this.deployParameters = buildDeployParameters(projectName, authToken, async, apVersion, activationExpr, forceRefreshOfSnapshots);
            return mockedTool;
        }

        @Override
        protected void checkStepControllerVersion() throws MojoExecutionException {
            //mock the check
        }
    }
}
