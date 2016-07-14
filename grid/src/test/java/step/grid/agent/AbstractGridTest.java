package step.grid.agent;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import step.grid.Grid;
import step.grid.client.GridClient;

public abstract class AbstractGridTest {

	protected Agent agent;
	
	protected Grid grid;
	
	protected GridClient client;
	
	int nTokens = 1;

	public AbstractGridTest() {
		super();
	}

	public AbstractGridTest(int nTokens) {
		super();
		this.nTokens = nTokens;
	}

	@Before
	public void init() throws Exception {
		grid = new Grid(8081);
		grid.start();
		
		agent = new Agent("http://localhost:8081", null, 8080);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("att1", "val1");
		agent.addTokens(nTokens, attributes, null);
		agent.start();

		client = new GridClient(grid);
	}
	
	@After
	public void tearDown() throws Exception {
		agent.stop();
		grid.stop();
		client.close();
	}
}
