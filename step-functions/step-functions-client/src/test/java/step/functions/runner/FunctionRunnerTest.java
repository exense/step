package step.functions.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.functions.io.Output;
import step.functions.runner.FunctionRunner.Context;

public class FunctionRunnerTest {

	@Test
	public void test() throws IOException {
		TestFunction f = new TestFunction();
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractOrganizableObject.NAME, "moustache");
		f.setAttributes(attributes);
		
		try(Context context = FunctionRunner.getContext(new TestFunctionType())) {
			Output<JsonObject> o = context.run(f, "{}");
			Assert.assertEquals("tache", o.getPayload().getString("mous"));
		}
	}
}
