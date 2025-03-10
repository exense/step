package step.core.execution;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import step.core.execution.model.ExecutionParameters;

import static org.junit.Assert.assertEquals;

public class ExecutionContextTests {

    private ExecutionContext context;

    @Before
    public void before() {
        // fresh context before every test
        context = new ExecutionContext(new ObjectId().toString(), new ExecutionParameters());
    }

    @Test
    public void testEmpty() {
        assertEquals("", context.getAgentUrls());
    }

    @Test
    public void testNullIgnored() {
        context.addAgentUrl(null);
        assertEquals("", context.getAgentUrls());
    }

    @Test
    public void testSingleAgent() {
        String url = "http://dummy.url";
        context.addAgentUrl(url);
        context.addAgentUrl(url);
        assertEquals(url, context.getAgentUrls());
    }

    @Test
    public void testMultipleAndSorting() {
        context.addAgentUrl("http://XyZ");
        context.addAgentUrl("http://ABCD");
        context.addAgentUrl("http://123");
        context.addAgentUrl("http://ABCD");
        context.addAgentUrl("http://abd");
        context.addAgentUrl("http://xy");
        assertEquals("http://123 http://ABCD http://abd http://xy http://XyZ", context.getAgentUrls());
    }
}
