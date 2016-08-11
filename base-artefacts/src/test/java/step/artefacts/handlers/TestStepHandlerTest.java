package step.artefacts.handlers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)  
public class TestStepHandlerTest extends AbstractArtefactHandlerTest {
	
//	TestStep s;
//	
//	@Mock
//	KeywordRepository keywordRepositoryMock;
//	
//	@Mock
//	AdapterClient adapterClient;
//	
//	QuotaManagerMock quotaManagerMock;
//	
	@Test
	public void test() throws Exception {
//		CallFunctionReportNode child = execute("<test/>", buildOutput("<Result />"), true);
//		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
	}
//	
//	@Test
//	public void testBusinessException() throws Exception {
//		CallFunctionReportNode child = execute("<test/>", outputBuilder().setBusinessError("Test Error").build(), true);
//		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
//	}
//	
//	@Test
//	public void testTechnicalException() throws Exception {
//		CallFunctionReportNode child = execute("<test/>", outputBuilder().setTechnicalError("Test Error").build(), true);
//		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
//	}
//	
//	@Test
//	public void testSkipped() throws Exception {
//		CallFunctionReportNode child = execute("<test Para1=\"SKIP\"/>", null, false);
//		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
//	}
//	
//	private void genericChecks(CallFunctionReportNode child) {
//		assertEquals(quotaManagerMock.getAcquiredPermit(), quotaManagerMock.getReleasedPermit());	
//		assertEquals(s.getInput(),child.getInput());
//	}
//	
//	private CallFunctionReportNode execute(String input, Output output, boolean adapterCall) throws Exception {
//		setupContext();
//
//		quotaManagerMock = new QuotaManagerMock();
//		
//		Keyword k = new Keyword("test");
//		k.setHasSchema(false);
//		k.setType(KeywordType.REMOTE);
//		
//		when(keywordRepositoryMock.getConfigurationForKeyword(anyObject(), anyString())).thenReturn(k);
//		
//		ProcessInputResponse r = adapterClient.new ProcessInputResponse();
//		r.setOutput(output);
//		
//		AtomicBoolean b = new AtomicBoolean(false);
//		when(adapterClient.processInput(anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<ProcessInputResponse>() {
//		    @Override
//		    public ProcessInputResponse answer(InvocationOnMock invocation) throws Throwable {
//		    	b.set(true);
//		    	return r;
//		    }
//		  });
//		
//		when(adapterClient.processInput(anyObject(), anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<ProcessInputResponse>() {
//		    @Override
//		    public ProcessInputResponse answer(InvocationOnMock invocation) throws Throwable {
//		    	b.set(true);
//		    	return r;
//		    }
//		  });
//		
//
//		putGlobalContext(GridPlugin.GRIDCLIENT_KEY, adapterClient);
//		putGlobalContext(GridPlugin.KEYWORD_REPOSITORY_KEY, keywordRepositoryMock);
//		putGlobalContext(QuotaManagerPlugin.QUOTAMANAGER_KEY, quotaManagerMock);
//		putVar("tec.handlers.step.useskips","true");	
//		putVar("tec.logtransactions","false");	
//		
//		s = add(new TestStep());
//		s.setInput(input);	
//		
//		execute(s);
//		
//		Assert.assertTrue(adapterCall?b.get():!b.get());
//		CallFunctionReportNode child = (CallFunctionReportNode) getFirstReportNode();
//		
//		genericChecks(child);
//		DocumentTransformer t = new DocumentTransformer();
//		assertEquals(output!=null?t.transform(output.getPayload()):null,child.getOutput());
//
//		return child;
//	}
//
//	
//	
//	private Output buildOutput(String output) throws ParserException {
//		OutputBuilder b = outputBuilder();
//		b.setPayload(output);
//		return b.build();
//	}
//
//	private OutputBuilder outputBuilder() {
//		OutputBuilder b = new OutputBuilder();
//		return b;
//	}
//
//	private void putGlobalContext(String key, Object value) {
//		ExecutionContext.getCurrentContext().getGlobalContext().put(key, value);
//	}
//
//	private void putVar(String key, String value) {
//		ExecutionContext.getCurrentContext().getVariablesManager().
//		putVariable(ExecutionContext.getCurrentContext().getCurrentReportNode(), key, value);
//	}
}

