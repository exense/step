package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;
import step.adapters.commons.helper.DocumentTransformer;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.adapters.commons.model.ParserException;
import step.artefacts.TestStep;
import step.artefacts.handlers.teststep.QuotaManagerMock;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.grid.client.AdapterClient;
import step.grid.client.AdapterClient.ProcessInputResponse;
import step.plugins.adaptergrid.AdapterClientPlugin;
import step.plugins.keywordrepository.Keyword;
import step.plugins.keywordrepository.KeywordRepository;
import step.plugins.keywordrepository.KeywordType;
import step.plugins.quotamanager.QuotaManagerPlugin;

@RunWith(MockitoJUnitRunner.class)  
public class TestStepHandlerTest extends AbstractArtefactHandlerTest {
	
	TestStep s;
	
	@Mock
	KeywordRepository keywordRepositoryMock;
	
	@Mock
	AdapterClient adapterClient;
	
	QuotaManagerMock quotaManagerMock;
	
	@Test
	public void test() throws Exception {
		TestStepReportNode child = execute("<test/>", buildOutput("<Result />"), true);
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
	}
	
	@Test
	public void testBusinessException() throws Exception {
		TestStepReportNode child = execute("<test/>", outputBuilder().setBusinessError("Test Error").build(), true);
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
	}
	
	@Test
	public void testTechnicalException() throws Exception {
		TestStepReportNode child = execute("<test/>", outputBuilder().setTechnicalError("Test Error").build(), true);
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
	}
	
	@Test
	public void testSkipped() throws Exception {
		TestStepReportNode child = execute("<test Para1=\"SKIP\"/>", null, false);
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
	}
	
	private void genericChecks(TestStepReportNode child) {
		assertEquals(quotaManagerMock.getAcquiredPermit(), quotaManagerMock.getReleasedPermit());	
		assertEquals(s.getInput(),child.getInput());
	}
	
	private TestStepReportNode execute(String input, Output output, boolean adapterCall) throws Exception {
		setupContext();

		quotaManagerMock = new QuotaManagerMock();
		
		Keyword k = new Keyword("test");
		k.setHasSchema(false);
		k.setType(KeywordType.REMOTE);
		
		when(keywordRepositoryMock.getConfigurationForKeyword(anyObject(), anyString())).thenReturn(k);
		
		ProcessInputResponse r = adapterClient.new ProcessInputResponse();
		r.setOutput(output);
		
		AtomicBoolean b = new AtomicBoolean(false);
		when(adapterClient.processInput(anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<ProcessInputResponse>() {
		    @Override
		    public ProcessInputResponse answer(InvocationOnMock invocation) throws Throwable {
		    	b.set(true);
		    	return r;
		    }
		  });
		
		when(adapterClient.processInput(anyObject(), anyObject(), anyObject(), anyObject())).thenAnswer(new Answer<ProcessInputResponse>() {
		    @Override
		    public ProcessInputResponse answer(InvocationOnMock invocation) throws Throwable {
		    	b.set(true);
		    	return r;
		    }
		  });
		

		putGlobalContext(AdapterClientPlugin.ADAPTER_CLIENT_KEY, adapterClient);
		putGlobalContext(AdapterClientPlugin.KEYWORD_REPOSITORY_KEY, keywordRepositoryMock);
		putGlobalContext(QuotaManagerPlugin.QUOTAMANAGER_KEY, quotaManagerMock);
		putVar("tec.handlers.step.useskips","true");	
		putVar("tec.logtransactions","false");	
		
		s = add(new TestStep());
		s.setInput(input);	
		
		execute(s);
		
		Assert.assertTrue(adapterCall?b.get():!b.get());
		TestStepReportNode child = (TestStepReportNode) getFirstReportNode();
		
		genericChecks(child);
		DocumentTransformer t = new DocumentTransformer();
		assertEquals(output!=null?t.transform(output.getPayload()):null,child.getOutput());

		return child;
	}

	
	
	private Output buildOutput(String output) throws ParserException {
		OutputBuilder b = outputBuilder();
		b.setPayload(output);
		return b.build();
	}

	private OutputBuilder outputBuilder() {
		OutputBuilder b = new OutputBuilder();
		return b;
	}

	private void putGlobalContext(String key, Object value) {
		ExecutionContext.getCurrentContext().getGlobalContext().put(key, value);
	}

	private void putVar(String key, String value) {
		ExecutionContext.getCurrentContext().getVariablesManager().
		putVariable(ExecutionContext.getCurrentContext().getCurrentReportNode(), key, value);
	}
}

