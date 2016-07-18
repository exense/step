package step.plugins.keywordrepository;

import junit.framework.Assert;

import org.junit.Test;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class KeywordRepositoryTest {

	@Test
	public void testGetConfiguration() {
		KeywordRepository repo = new KeywordRepository();
		ExecutionContext c = new ExecutionContext("");
		ReportNode node = new ReportNode();
		ExecutionContext.setCurrentReportNode(node);
		Keyword k = repo.getConfigurationForKeyword(c, "kwLocal");
		Assert.assertEquals(k.getType(), KeywordType.LOCAL);
		Assert.assertEquals(k.hasSchema(), true);
		
		k = repo.getConfigurationForKeyword(c, "kwRemote");
		Assert.assertEquals(k.getType(), KeywordType.REMOTE);
		
		k = repo.getConfigurationForKeyword(c, "kwWithoutSchema");
		Assert.assertEquals(k.hasSchema(), false);
		
		k = repo.getConfigurationForKeyword(c, "kwActivationTest");
		Assert.assertNull(k);
		
		c.getVariablesManager().putVariable(node, "TestVar", true);
		k = repo.getConfigurationForKeyword(c, "kwActivationTest");
		Assert.assertNotNull(k);
		
		k = repo.getConfigurationForKeyword(c, "kwSelectionPatterns");
		Assert.assertEquals(".*",k.getSelectionPatterns().get("pattern1"));
		
		k = repo.getConfigurationForKeyword(c, "kwSelectionAttributes");
		Assert.assertEquals("val1",k.getAttributes().get("att1"));
		
		Assert.assertEquals(6,repo.getKeywordList().size());
		
		repo.destroy();
	}
}
