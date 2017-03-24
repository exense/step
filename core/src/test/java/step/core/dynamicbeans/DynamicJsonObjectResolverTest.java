package step.core.dynamicbeans;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.expressions.ExpressionHandler;

public class DynamicJsonObjectResolverTest {

	DynamicJsonObjectResolver resolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()));
	
	@Test
	public void test1() throws JsonProcessingException {
		TestBean bean = new TestBean();
		
		ObjectMapper m = new ObjectMapper();
		String jsonStr = m.writeValueAsString(bean);
		JsonObject o = Json.createReader(new StringReader(jsonStr)).readObject();
		
		JsonObject output = resolver.evaluate(o, null);
		Assert.assertEquals("test", output.getString("testString"));
		Assert.assertEquals(true, output.getBoolean("testBoolean"));
		Assert.assertEquals(10, output.getInt("testInteger"));
		Assert.assertEquals("test", ((JsonObject)output.getJsonArray("testArray").get(0)).getString("testString"));
		Assert.assertEquals("test", output.getJsonObject("testRecursive2").getString("testString"));
	}
	
	
}
