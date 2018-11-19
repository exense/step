package step.functions.io;

import java.io.IOException;

import javax.json.JsonObject;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.functions.handler.FunctionInputOutputObjectMapperFactory;

public class OutputSerializationTest {

	@Test
	public void test() throws IOException {
		OutputBuilder builder = new OutputBuilder();
		builder.add("test", "test");
		Output<JsonObject> output = builder.build();
		ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
		String value = mapper.writeValueAsString(output);
		mapper.readValue(value, new TypeReference<Output<JsonObject>>() {});
	}
}
