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
import step.cli.ExecuteAutomationPackageTool;
import step.cli.parameters.ApExecuteParameters;

import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.assertEquals;

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
		assertEquals("http://localhost:8080", mojo.url);
		assertEquals("abc", mojo.params.getAuthToken());
		assertEquals(TENANT_1.getName(), mojo.params.getStepProjectName());
		assertEquals("testUser", mojo.params.getUserId());
		assertEquals((Integer) 3, mojo.params.getExecutionResultTimeoutS());
		assertEquals(true, mojo.params.getWaitForExecution());
		assertEquals(ensureExecutionSuccess, mojo.params.getEnsureExecutionSuccess());
        assertEquals(ExecuteAutomationPackageTool.ReportType.junit, mojo.params.getReports().get(0).getReportType());
        assertEquals(List.of(ExecuteAutomationPackageTool.ReportOutputMode.stdout, ExecuteAutomationPackageTool.ReportOutputMode.file), mojo.params.getReports().get(0).getOutputModes());
		assertEquals(createTestCustomParams(), mojo.params.getExecutionParameters());
		assertEquals(TEST_INCLUDE_PLANS, mojo.params.getIncludePlans());
		Assert.assertNull(TEST_INCLUDE_PLANS, mojo.params.getExcludePlans());
		assertEquals(TEST_INCLUDE_CATEGORIES, mojo.params.getIncludeCategories());
		assertEquals(TEST_EXCLUDE_CATEGORIES, mojo.params.getExcludeCategories());
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

		ExecuteAutomationPackageMojo.ReportParam reportParam = new ExecuteAutomationPackageMojo.ReportParam();
		reportParam.setType(ExecuteAutomationPackageTool.ReportType.junit);
		reportParam.setOutput("stdout,file");
		mojo.setReports(List.of(reportParam));
		mojo.setReportDir("C://temp");

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

	// --- parseReports(String raw) tests ---

	@Test
	public void parseReportsRaw_withNull_returnsNull() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		Assert.assertNull(mojo.callParseReportsRaw(null));

		mojo.execute();
	}

	@Test
	public void parseReportsRaw_withBlankString_returnsNull() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		String raw = "   ";
		mojo.setReportsRaw(raw);
		Assert.assertNull(mojo.callParseReportsRaw(raw));

		mojo.execute();
	}

	@Test
	public void parseReportsRaw_withSingleEntry_returnsOneReport() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		String raw = "junit:file";
		mojo.setReportsRaw(raw);
		List<ExecuteAutomationPackageMojo.ReportParam> result = mojo.callParseReportsRaw(raw);

		assertEquals(1, result.size());
		assertEquals(ExecuteAutomationPackageTool.ReportType.junit, result.get(0).getType());
		assertEquals("file", result.get(0).getOutput());

		mojo.execute();
	}

	@Test
	public void parseReportsRaw_withMultipleEntries_returnsMultipleReports() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		String raw = "junit:file,stdout;aggregated:file,stdout";
		mojo.setReportsRaw(raw);
		List<ExecuteAutomationPackageMojo.ReportParam> result = mojo.callParseReportsRaw(raw);

		assertEquals(2, result.size());
		assertEquals(ExecuteAutomationPackageTool.ReportType.junit, result.get(0).getType());
		assertEquals("file,stdout", result.get(0).getOutput());
		assertEquals(ExecuteAutomationPackageTool.ReportType.aggregated, result.get(1).getType());
		assertEquals("file,stdout", result.get(1).getOutput());

		mojo.execute();
	}

	@Test
	public void parseReportsRaw_withEmptyOutput_returnsReportWithEmptyOutput() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		String raw = "junit:";
		mojo.setReportsRaw(raw);
		List<ExecuteAutomationPackageMojo.ReportParam> result = mojo.callParseReportsRaw(raw);

		assertEquals(1, result.size());
		assertEquals(ExecuteAutomationPackageTool.ReportType.junit, result.get(0).getType());
		assertEquals("", result.get(0).getOutput());

		mojo.execute();
	}

	@Test
	public void parseReportsRaw_withInvalidFormat_throwsIllegalArgumentException() throws MojoExecutionException {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		String raw = "junit";
		mojo.setReportsRaw(raw);
		IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> mojo.callParseReportsRaw(raw));
		String expectedErrorMessage = "Invalid report format 'junit', expected 'type:outputModes'. Multiple reports should be separated by a semicolon ';', multiple output modes can be separated by a comma ',' (ex: junit:file,stdout;aggregated:stdout).";
		assertEquals(expectedErrorMessage, e.getMessage());

		MojoExecutionException mojoExecutionException = Assert.assertThrows(MojoExecutionException.class, () -> mojo.execute());
		assertEquals(expectedErrorMessage, mojoExecutionException.getCause().getMessage());
	}

	// --- reportsRaw precedence tests via execute() ---

	@Test
	public void execute_withReportsRaw_andNoXmlReports_usesRawReports() throws Exception {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		mojo.setReportsRaw("junit:stdout");

		mojo.execute();

		Assert.assertNotNull(mojo.params.getReports());
		assertEquals(1, mojo.params.getReports().size());
		assertEquals(ExecuteAutomationPackageTool.ReportType.junit, mojo.params.getReports().get(0).getReportType());
		assertEquals(List.of(ExecuteAutomationPackageTool.ReportOutputMode.stdout), mojo.params.getReports().get(0).getOutputModes());
	}

	@Test
	public void execute_withBothXmlReportsAndReportsRaw_rawReportsTakePrecedence() throws Exception {
		ExecuteAutomationPackageMojoTestable mojo = new ExecuteAutomationPackageMojoTestable();
		configureMinimalMojoForReportsTest(mojo);
		// XML reports: junit with file output (should be ignored)
		ExecuteAutomationPackageMojo.ReportParam xmlReport = new ExecuteAutomationPackageMojo.ReportParam();
		xmlReport.setType(ExecuteAutomationPackageTool.ReportType.junit);
		xmlReport.setOutput("file");
		mojo.setReports(List.of(xmlReport));
		// reportsRaw (system property): junit with stdout output — takes precedence
		mojo.setReportsRaw("junit:stdout");

		mojo.execute();

		assertEquals(1, mojo.params.getReports().size());
		assertEquals(List.of(ExecuteAutomationPackageTool.ReportOutputMode.stdout),
				mojo.params.getReports().get(0).getOutputModes());
	}

	private void configureMinimalMojoForReportsTest(ExecuteAutomationPackageMojoTestable mojo) {
		mojo.setArtifactId(ARTIFACT_ID);
		mojo.setArtifactGroupId(GROUP_ID);
		mojo.setArtifactVersion(VERSION_ID);
		mojo.setUrl("http://localhost:8080");
		mojo.setReportDir("C://temp");
	}

	private static class ExecuteAutomationPackageMojoTestable extends ExecuteAutomationPackageMojo {

		private final ExecuteAutomationPackageTool mockedTool = Mockito.mock(ExecuteAutomationPackageTool.class);

		private String url;
		private ApExecuteParameters params;

		public ExecuteAutomationPackageMojoTestable() {
			super();
		}

		public List<ReportParam> callParseReportsRaw(String raw) {
			return parseReports(raw);
		}

		@Override
		protected ExecuteAutomationPackageTool createTool(String url, ApExecuteParameters params) {
			this.url = url;
			this.params = params;
			return mockedTool;
		}

		@Override
		protected void checkStepControllerVersion() throws MojoExecutionException {
			//Mock check
		}
	}
}