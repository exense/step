package step.planbuilder;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

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
		return call;
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
