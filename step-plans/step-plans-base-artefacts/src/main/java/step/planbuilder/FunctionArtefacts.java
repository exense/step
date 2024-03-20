/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.planbuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

import java.util.Map;

import static step.core.accessors.AbstractOrganizableObject.NAME;

public class FunctionArtefacts {

	private static final Logger logger = LoggerFactory.getLogger(FunctionArtefacts.class);
	
	public static FunctionGroup session() {
		return new FunctionGroup();
	}
	
	public static CallFunction keywordWithDynamicInput(String keywordName, String input) {
		return keywordWithDynamicInput(keywordName, true, input);
	}

	
	public static CallFunction keywordWithDynamicInput(String keywordName, boolean remote, String input) {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<>(input,""));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		call.setRemote(new DynamicValue<>(remote));
		return call;
	}
	
	public static CallFunction keywordWithKeyValues(String keywordName, String... keyValues) {
		return keywordWithKeyValues(keywordName, true, keyValues);
	}

	
	public static CallFunction keywordWithKeyValues(String keywordName, boolean remote, String... keyValues) {
		CallFunction call = new CallFunction();
		
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if(keyValues!=null) {
			for(int i=0;i<keyValues.length;i+=2) {
				builder.add(keyValues[i], keyValues[i+1]);
			}			
		}
		
		call.setArgument(new DynamicValue<>(builder.build().toString()));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		call.setRemote(new DynamicValue<>(remote));
		return call;
	}
	
	
	public static CallFunction keywordWithDynamicKeyValues(String keywordName, String... keyValues) {
		return keywordWithDynamicKeyValues(keywordName, true, keyValues);
	}
	
	public static CallFunction keywordWithDynamicKeyValues(String keywordName, boolean remote, String... keyValues) {
		CallFunction call = new CallFunction();
		
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if(keyValues!=null) {
			for(int i=0;i<keyValues.length;i+=2) {
				JsonObjectBuilder dynamicExpressionBuilder = Json.createObjectBuilder();
				dynamicExpressionBuilder.add("dynamic", true);
				dynamicExpressionBuilder.add("expression", keyValues[i+1]);
				builder.add(keyValues[i], dynamicExpressionBuilder.build());
			}			
		}
		
		call.setArgument(new DynamicValue<>(builder.build().toString()));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		call.setRemote(new DynamicValue<>(remote));
		return call;
	}

	
	public static CallFunction keyword(String keywordName, String input) {
		return keyword(keywordName, true, input);
	}
	
	public static CallFunction keyword(String keywordName, boolean remote, String input) {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<>(input));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		call.getAttributes().put("name", keywordName);
		call.setRemote(new DynamicValue<>(remote));
		return call;
	}

	public static CallFunction keyword(Map<String,String> selectionCriteria, boolean remote, String input) throws JsonProcessingException {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<>(input));
		ObjectMapper mapper = new ObjectMapper();
		call.getFunction().setValue(mapper.writeValueAsString(selectionCriteria));
		call.getAttributes().put(NAME, selectionCriteria.get(NAME));
		call.setRemote(new DynamicValue<>(remote));
		return call;
	}
	
	public static Assert assertion(DynamicValue<String> actual, AssertOperator operator, DynamicValue<String> expected) {
		Assert assertion = new Assert();
		assertion.setActual(actual);
		assertion.setOperator(operator);
		assertion.setExpected(expected);
		return assertion;
	}
	
	public static DynamicValue<String> dynamic(String dynamic) {
		return new DynamicValue<>(dynamic, "");
	}

	/**
	 * @deprecated Calling keywords by ID is deprecated since 3.18. Use the call by attributes instead: keyword()
	 */
	@Deprecated
	public static CallFunction keywordById(String keywordId, boolean remote, String input) {
		throw new RuntimeException("Calling keywords by ID is deprecated since 3.18.");
	}

	/**
	 * @deprecated Calling keywords by ID is deprecated since 3.18. Use the call by attributes instead: keyword()
	 */
	@Deprecated
	public static CallFunction keywordById(String keywordId, String input) {
		return keywordById(keywordId, true, input);
	}

	public static CallFunction keyword(String keywordName) {
		return keyword(keywordName, true);
	}
	
	public static CallFunction keyword(String keywordName, boolean remote) {
		return keyword(keywordName, remote, "{}");
	}

	public static CallFunctionBuilder keyword(Map<String, String> attributes) {
		JsonObjectBuilder attributeJson = Json.createObjectBuilder();
		attributes.forEach((key, value) -> {
			JsonObject dynamicExpression = Json.createObjectBuilder().add("value", value).add("dynamic", false).build();
			attributeJson.add(key, dynamicExpression);
		});
		return new CallFunctionBuilder(attributeJson.build().toString());
	}

	public static class CallFunctionBuilder {

		private final DynamicValue<String> function;
		private DynamicValue<String> input;

		public CallFunctionBuilder(String function) {
			this.function = new DynamicValue<>(function);
		}

		public CallFunctionBuilder withInput(String input) {
			this.input = new DynamicValue<>(input);
			return this;
		}

		public CallFunction build() {
			CallFunction callFunction = new CallFunction();
			callFunction.setFunction(function);
			callFunction.setArgument(input);
			return callFunction;
		}
	}
}
