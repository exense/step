package step.grid;

import org.junit.Assert;
import org.junit.Test;

public class ExpiringMapTest {

	@Test
	public void test() throws InterruptedException {
		ExpiringMap<String, String> m = new ExpiringMap<>(10, 1);
		
		m.put("test", "test");
		m.put("test2", "test");
		Assert.assertEquals("test", m.get("test"));
		Thread.sleep(100);;
		
		Assert.assertNull(m.get("test"));
		
		m.put("test", "test");
		Assert.assertEquals("test", m.get("test"));
		
		for(int i=0;i<100;i++) {
			Thread.sleep(1);
			m.touch("test");
		}
		
		Assert.assertEquals("test", m.get("test"));

		
		m.remove("test");
		Assert.assertNull(m.get("test"));
		Assert.assertFalse(m.containsKey("test"));
		
	}

}
