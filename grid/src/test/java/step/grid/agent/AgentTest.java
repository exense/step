package step.grid.agent;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import step.grid.Grid;

public class AgentTest {

	@Test
	public void test() throws InterruptedException {
		Agent agent = new Agent("http://localhost:8081", null, 8080);
		Map<String, String> attributes = new HashMap<>();
		attributes.put("att1", "val1");
		agent.addTokens(1, attributes, null);
		(new Thread(new Runnable() {
			@Override
			public void run() {
				agent.run();
			}
		})).start();
		
		Grid grid = new Grid(8081);
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					grid.start();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		})).start();
		
		synchronized (this) {
			wait();			
		}
	}

}
