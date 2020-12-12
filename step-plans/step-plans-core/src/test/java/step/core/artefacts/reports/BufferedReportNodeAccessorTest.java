package step.core.artefacts.reports;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BufferedReportNodeAccessorTest {

	@Test
	public void test() throws Exception {
		InMemoryReportNodeAccessor inMemoryReportNodeAccessor = new InMemoryReportNodeAccessor();
		BufferedReportNodeAccessor bufferedReportNodeAccessor = new BufferedReportNodeAccessor(inMemoryReportNodeAccessor, null);
		
		int count = 10003;
		for(int i=0;i<count;i++) {
			bufferedReportNodeAccessor.save(new ReportNode());
		}
		
		bufferedReportNodeAccessor.close();
		
		Map<String, ReportNode> result = new HashMap<>();
		inMemoryReportNodeAccessor.getAll().forEachRemaining(n->result.put(n.getId().toString(), n));
		
		
		
		assertEquals(count, result.size());
	}

}
