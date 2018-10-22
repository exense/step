package step.grid;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

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
	
	@Test
	public void testEntrySet() throws InterruptedException {
		ExpiringMap<String, String> m = new ExpiringMap<>(10, 1);
		
		m.put("test", "value");
		m.put("test2", "value");
		
		Set<Entry<String, String>> entrySet = m.entrySet();
		Assert.assertEquals(2, entrySet.size());
		entrySet.forEach(e->{
			assert e.getKey().equals("test") || e.getKey().equals("test2");
			assert e.getValue().equals("value");
		});
		
		m.clear();
		Assert.assertEquals(0, m.size());
		Assert.assertEquals(0, m.entrySet().size());
		
		m.put("test", "value");
		Collection<String> values = m.values();
		Assert.assertEquals(1, values.size());
		values.contains("value");
	}
}
