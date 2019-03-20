package step.core.artefacts;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;
import step.core.deployment.JacksonMapperProvider;

public class MapDeserializerTest {

	@Test
	public void test() throws IOException {
		ObjectMapper m = JacksonMapperProvider.createMapper();
		
		TestBean b = new TestBean();
		b.getMap().put("key1", new TestBean2());
		b.getMap().put("key2", new TestBean2());
		b.getMap().put("key3", null);

		TestBean b2 = m.readValue(m.writeValueAsString(b), TestBean.class);
		Assert.assertEquals("Test", ((TestBean2)b2.getMap().get("key1")).getTest());
		Assert.assertEquals("Test", ((TestBean2)b2.getMap().get("key2")).getTest());
		Assert.assertNull(b2.getMap().get("key3"));
	}

}
