package step.planbuilder;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.core.dynamicbeans.DynamicValue;

public class FunctionArtefacts {
	
	public static FunctionGroup session() {
		FunctionGroup call = new FunctionGroup();
		return call;
	}
	
	public static CallFunction keywordWithDynamicInput(String keywordName, String input) {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<String>(input,""));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		return call;
	}
	
	public static CallFunction keywordWithKeyValues(String keywordName, String... keyValues) {
		CallFunction call = new CallFunction();
		
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if(keyValues!=null) {
			for(int i=0;i<keyValues.length;i+=2) {
				builder.add(keyValues[i], keyValues[i+1]);
			}			
		}
		
		call.setArgument(new DynamicValue<String>(builder.build().toString()));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		return call;
	}
	
	public static CallFunction keywordWithDynamicKeyValues(String keywordName, String... keyValues) {
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
		
		call.setArgument(new DynamicValue<String>(builder.build().toString()));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		return call;
	}
	
	public static CallFunction keyword(String keywordName, String input) {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<String>(input));
		call.getFunction().setValue("{\"name\":\""+keywordName+"\"}");
		call.getAttributes().put("name", keywordName);
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
	
	public static CallFunction keywordById(String keywordId, String input) {
		CallFunction call = new CallFunction();
		call.setArgument(new DynamicValue<String>(input));
		call.setFunctionId(keywordId);
		return call;
	}
	
	public static CallFunction keyword(String keywordName) {
		return keyword(keywordName, "{}");
	}

}
