package step.cli;

import ch.exense.commons.io.FileHelper;
import org.bson.types.ObjectId;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.artefacts.Echo;
import step.artefacts.reports.EchoReportNode;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.controller.multitenancy.Tenant;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.filters.PlanByExcludedCategoriesFilter;
import step.core.plans.filters.PlanByIncludedCategoriesFilter;
import step.core.plans.filters.PlanByIncludedNamesFilter;
import step.core.plans.filters.PlanMultiFilter;
import step.core.repositories.ImportResult;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class AbstractExecuteAutomationPackageToolTest {

    protected static final Tenant TENANT_1 = createTenant1();

    public static final String TEST_INCLUDE_PLANS = "plan1,plan2";
    public static final String TEST_INCLUDE_CATEGORIES = "PerformanceTest,JMterTest";
    public static final String TEST_EXCLUDE_CATEGORIES = "CypressTest,OidcTest";

    private File tempReportFolder;

    @Before
    public void before() throws IOException {
        // create new temp folder before each test to be sure it is empty
        this.tempReportFolder = Files.createTempDirectory("automation-package-tool-test").toFile();
    }

    @After
    public void after() {
        if (this.tempReportFolder != null) {
            FileHelper.deleteFolder(this.tempReportFolder);
        }
    }

    @Test
    public void testExecuteOk() throws Exception {
        Execution execution = getMockedExecution(ReportNodeStatus.PASSED);

        List<Execution> executions = List.of(execution);
        List<String> executionIds = getExecuteAutomationPackageResult(executions);

        RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

        RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

        ExecuteAutomationPackageToolTestable tool = createTool(true, remoteExecutionManagerMock, remoteAutomationPackageClientMock);
        tool.execute();

        assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);

        File[] storedReports = tempReportFolder.listFiles((dir, name) -> name.matches(".*-aggregated.txt"));
        Assert.assertNotNull(storedReports);
        Assert.assertEquals(1, storedReports.length);
        Assert.assertEquals("Echo: 1x: PASSED > Hello\n", new String(Files.readAllBytes(storedReports[0].toPath())));
    }

    private static List<String> getExecuteAutomationPackageResult(List<Execution> executions) {
        return executions.stream().map(e -> e.getId().toString()).collect(Collectors.toList());
    }

    @Test
    public void testExecuteImportError() throws Exception {
        Execution execution = getMockedExecution(ReportNodeStatus.FAILED, "Import error");

        List<Execution> executions = List.of(execution);
        List<String> executionIds = getExecuteAutomationPackageResult(executions);

        RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

        RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

        ExecuteAutomationPackageToolTestable tool = createTool(true, remoteExecutionManagerMock, remoteAutomationPackageClientMock);
        Assert.assertThrows(StepCliExecutionException.class, tool::execute);
        assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
    }

    @Test
    public void testExecuteNotOk() throws Exception {
        Execution execution = getMockedExecution(ReportNodeStatus.FAILED);

        List<Execution> executions = List.of(execution);
        List<String> executionIds = getExecuteAutomationPackageResult(executions);

        RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

        RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

        ExecuteAutomationPackageToolTestable tool = createTool(true, remoteExecutionManagerMock, remoteAutomationPackageClientMock);
        Assert.assertThrows(StepCliExecutionException.class, tool::execute);
        assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
    }

    @Test
    public void testExecuteNotOkWithout() throws Exception {
        Execution execution = getMockedExecution(ReportNodeStatus.FAILED);

        List<Execution> executions = List.of(execution);
        List<String> executionIds = getExecuteAutomationPackageResult(executions);

        RemoteExecutionManager remoteExecutionManagerMock = createExecutionManagerMock(executions);

        RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteAutomationPackageClientMock.executeAutomationPackage(Mockito.any(), Mockito.any())).thenReturn(executionIds);

        ExecuteAutomationPackageToolTestable tool = createTool(false, remoteExecutionManagerMock, remoteAutomationPackageClientMock);
        tool.execute();
        assertAutomationPackageClientMockCalls(remoteAutomationPackageClientMock);
    }

    private static void assertAutomationPackageClientMockCalls(RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) throws AutomationPackageClientException {
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<IsolatedAutomationPackageExecutionParameters> executionParamsCaptor = ArgumentCaptor.forClass(IsolatedAutomationPackageExecutionParameters.class);
        Mockito.verify(remoteAutomationPackageClientMock, Mockito.times(1)).executeAutomationPackage(fileCaptor.capture(), executionParamsCaptor.capture());
        File capturedFile = fileCaptor.getValue();
        Assert.assertEquals("test-file-jar-with-dependencies.jar", capturedFile.getName());

        IsolatedAutomationPackageExecutionParameters captured = executionParamsCaptor.getValue();
        Assert.assertEquals("testUser", captured.getUserID());
        Assert.assertEquals(ExecutionMode.RUN, captured.getMode());
        PlanMultiFilter planFilter = (PlanMultiFilter) captured.getPlanFilter();
        PlanMultiFilter expectedFilter = new PlanMultiFilter(List.of(new PlanByIncludedNamesFilter(Arrays.asList(TEST_INCLUDE_PLANS.split(","))),
                new PlanByIncludedCategoriesFilter(Arrays.asList(TEST_INCLUDE_CATEGORIES.split(","))),
                new PlanByExcludedCategoriesFilter(Arrays.asList(TEST_EXCLUDE_CATEGORIES.split(",")))));
        Assert.assertEquals(expectedFilter, planFilter);
        Assert.assertEquals(createTestCustomParams(), captured.getCustomParameters());
    }

    private static Execution getMockedExecution(ReportNodeStatus resultStatus) {
        return getMockedExecution(resultStatus, null);
    }

    private static Execution getMockedExecution(ReportNodeStatus resultStatus, String importError) {
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getId()).thenReturn(new ObjectId());
        Mockito.when(execution.getDescription()).thenReturn("My execution");
        Mockito.when(execution.getStatus()).thenReturn(ExecutionStatus.ENDED);
        Mockito.when(execution.getResult()).thenReturn(resultStatus);

        ImportResult t = new ImportResult();
        if(importError != null) {
            t.setErrors(List.of(importError));
            t.setSuccessful(false);
        } else {
            t.setSuccessful(true);
        }
        Mockito.when(execution.getImportResult()).thenReturn(t);
        return execution;
    }

    private RemoteExecutionManager createExecutionManagerMock(List<Execution> executions) throws TimeoutException, InterruptedException {
        RemoteExecutionManager remoteExecutionManagerMock = Mockito.mock(RemoteExecutionManager.class);
        Mockito.when(remoteExecutionManagerMock.get(Mockito.any())).thenAnswer(invocationOnMock -> executions.stream().filter(e -> e.getId().toString().equals(invocationOnMock.getArgument(0))).findFirst().get());
        Mockito.when(remoteExecutionManagerMock.waitForTermination(Mockito.anyList(), Mockito.anyLong())).thenReturn(executions);
        RemoteExecutionFuture futureMock = Mockito.mock(RemoteExecutionFuture.class);
        Mockito.when(futureMock.getErrorSummary()).thenReturn("Error summary...");
        Mockito.when((remoteExecutionManagerMock.getFuture(Mockito.anyString()))).thenReturn(futureMock);
        EchoReportNode echoReportNode = new EchoReportNode();
        echoReportNode.setEcho("Hello");
        Mockito.when(remoteExecutionManagerMock.getAggregatedReportView(Mockito.anyString())).thenReturn(new AggregatedReportView(new Echo(), "hash", Map.of("PASSED",1L), List.of(), ParentSource.MAIN, echoReportNode));
        return remoteExecutionManagerMock;
    }

    private ExecuteAutomationPackageToolTestable createTool(boolean ensureExecutionSuccess,
                                                            RemoteExecutionManager executionManagerMock,
                                                            RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) throws URISyntaxException {
        return new ExecuteAutomationPackageToolTestable(
                "http://localhost:8080",
                new AbstractExecuteAutomationPackageTool.Params()
                        .setStepProjectName(TENANT_1.getName())
                        .setUserId("testUser")
                        .setAuthToken("abc")
                        .setExecutionParameters(createTestCustomParams())
                        .setExecutionResultTimeoutS(2)
                        .setWaitForExecution(true)
                        .setEnsureExecutionSuccess(ensureExecutionSuccess)
                        .setReports(List.of(
                                new AbstractExecuteAutomationPackageTool.Report(
                                        AbstractExecuteAutomationPackageTool.ReportType.aggregated,
                                        List.of(AbstractExecuteAutomationPackageTool.ReportOutputMode.stdout, AbstractExecuteAutomationPackageTool.ReportOutputMode.file)))
                        )
                        .setReportOutputDir(tempReportFolder)
                        .setIncludePlans(TEST_INCLUDE_PLANS)
                        .setExcludePlans(null)
                        .setIncludeCategories(TEST_INCLUDE_CATEGORIES)
                        .setExcludeCategories(TEST_EXCLUDE_CATEGORIES)
                        .setWrapIntoTestSet(false)
                        .setNumberOfThreads(0),
                executionManagerMock, remoteAutomationPackageClientMock
        );
    }

    private static Map<String, String> createTestCustomParams() {
        Map<String, String> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");
        return params;
    }

    private static class ExecuteAutomationPackageToolTestable extends AbstractExecuteAutomationPackageTool {

        private final RemoteExecutionManager remoteExecutionManagerMock;
        private final RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;

        public ExecuteAutomationPackageToolTestable(String url, Params params,
                                                    RemoteExecutionManager remoteExecutionManagerMock,
                                                    RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) {
            super(url, params);
            this.remoteExecutionManagerMock = remoteExecutionManagerMock;
            this.remoteAutomationPackageClientMock = remoteAutomationPackageClientMock;
        }

        @Override
        protected File getAutomationPackageFile() throws StepCliExecutionException {
            return new File("test-file-jar-with-dependencies.jar");
        }

        @Override
        protected RemoteExecutionManager createRemoteExecutionManager() {
            return remoteExecutionManagerMock;
        }

        @Override
        protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
            return remoteAutomationPackageClientMock;
        }
    }

    protected static Tenant createTenant1() {
        Tenant tenant1 = new Tenant();
        tenant1.setName("project1");
        tenant1.setProjectId(new ObjectId().toString());
        return tenant1;
    }

}