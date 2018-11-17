package step.functions.execution;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

import step.functions.Input;
import step.functions.Output;

/**
 * Factory used to create the {@link ObjectMapper} used to serialize/deserialize 
 * {@link Input} and {@link Output} instances
 *
 */
public class FunctionInputOutputObjectMapperFactory {

	public static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JSR353Module());
		return mapper;
	}
}
