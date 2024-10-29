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
import step.automation.packages.client.AutomationPackageClientException;
import step.cli.AbstractExecuteAutomationPackageTool;

import java.net.URISyntaxException;
import java.util.*;

public class ExecuteAutomationPackageMojoTest extends AbstractMojoTest {

	public static final String TEST_INCLUDE_CATEGORIES = "PerformanceTest,JMterTest";
	public static final String TEST_EXCLUDE_CATEGORIES = "CypressTest,OidcTest";

	@Test
	public void testExecute() throws Exception {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();

		configureMojo(mojo, true);
		mojo.execute();
		assertToolCall(mojo, true);
	}

	private static void assertToolCall(ExecuteAutomationPackageMojoTestable mojo, Boolean ensureExecutionSuccess) throws AutomationPackageClientException {
		Mockito.verify(mojo.mockedTool, Mockito.times(1)).execute();
		Assert.assertEquals("http://localhost:8080", mojo.url);
		Assert.assertEquals("abc", mojo.authToken);
		Assert.assertEquals(TENANT_1.getName(), mojo.projectName);
		Assert.assertEquals("testUser", mojo.userId);
		Assert.assertEquals((Integer) 3, mojo.executionResultTimeoutS);
		Assert.assertEquals(true, mojo.waitForExecution);
		Assert.assertEquals(ensureExecutionSuccess, mojo.ensureExecutionSuccess);
		Assert.assertEquals(createTestCustomParams(), mojo.parameters);
		Assert.assertEquals(TEST_INCLUDE_PLANS, mojo.includePlans);
		Assert.assertNull(TEST_INCLUDE_PLANS, mojo.excludePlans);
		Assert.assertEquals(TEST_INCLUDE_CATEGORIES, mojo.includeCategories);
		Assert.assertEquals(TEST_EXCLUDE_CATEGORIES, mojo.excludeCategories);
	}

	private void configureMojo(ExecuteAutomationPackageMojoTestable mojo, boolean ensureExecutionSuccess) throws URISyntaxException {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactClassifier("jar-with-dependencies");
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setArtifactGroupId(GROUP_ID);
		mojo.setUrl("http://localhost:8080");
		mojo.setAuthToken("abc");
		mojo.setBuildFinalName("Test build name");
		mojo.setProjectVersion("1.0.0");
		mojo.setExecutionResultTimeoutS(3);
		mojo.setUserId("testUser");
		Map<String, String> params = createTestCustomParams();
		mojo.setExecutionParameters(params);
		mojo.setWaitForExecution(true);
		mojo.setEnsureExecutionSuccess(ensureExecutionSuccess);

		mojo.setIncludePlans(TEST_INCLUDE_PLANS);
		mojo.setIncludeCategories(TEST_INCLUDE_CATEGORIES);
		mojo.setExcludeCategories(TEST_EXCLUDE_CATEGORIES);

		MavenProject mockedProject = Mockito.mock(MavenProject.class);
		Mockito.when(mockedProject.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(mockedProject.getGroupId()).thenReturn(GROUP_ID);
		Mockito.when(mockedProject.getVersion()).thenReturn(VERSION_ID);

		Artifact mainArtifact = createArtifactMock();

		Mockito.when(mockedProject.getArtifact()).thenReturn(mainArtifact);

		Artifact jarWithDependenciesArtifact = createArtifactWithDependenciesMock();

		Mockito.when(mockedProject.getArtifacts()).thenReturn(Set.of(mainArtifact, jarWithDependenciesArtifact));
		Mockito.when(mockedProject.getAttachedArtifacts()).thenReturn(Arrays.asList(mainArtifact, jarWithDependenciesArtifact));

		mojo.setProject(mockedProject);

		mojo.setStepProjectName(TENANT_1.getName());
	}

	private static Map<String, String> createTestCustomParams() {
		Map<String, String> params = new HashMap<>();
		params.put("param1", "value1");
		params.put("param2", "value2");
		return params;
	}

	private static class ExecuteAutomationPackageMojoTestable extends ExecuteAutomationPackageMojo {

		private final AbstractExecuteAutomationPackageTool mockedTool = Mockito.mock(AbstractExecuteAutomationPackageTool.class);

		private String url;
		private String projectName;
		private String userId;
		private String authToken;
		private Map<String, String> parameters;
		private Integer executionResultTimeoutS;
		private Boolean waitForExecution;
		private Boolean ensureExecutionSuccess;
		private String includePlans;
		private String excludePlans;
		private String includeCategories;
		private String excludeCategories;

		public ExecuteAutomationPackageMojoTestable() {
			super();
		}

		@Override
		protected AbstractExecuteAutomationPackageTool createTool(String url, String projectName, String userId, String authToken, Map<String, String> parameters,
										Integer executionResultTimeoutS, Boolean waitForExecution, Boolean ensureExecutionSuccess, String includePlans, String excludePlans,
										String includeCategories, String excludeCategories, final Boolean wrapIntoTestSet, final Integer numberOfThreads) {
			this.url = url;
			this.projectName = projectName;
			this.userId = userId;
			this.authToken = authToken;
			this.parameters = parameters;
			this.executionResultTimeoutS = executionResultTimeoutS;
			this.waitForExecution = waitForExecution;
			this.ensureExecutionSuccess = ensureExecutionSuccess;
			this.includePlans = includePlans;
			this.excludePlans = excludePlans;
			this.includeCategories = includeCategories;
			this.excludeCategories = excludeCategories;
			return mockedTool;
		}

		@Override
		protected void checkStepControllerVersion() throws MojoExecutionException {
			//Mock check
		}
	}
}