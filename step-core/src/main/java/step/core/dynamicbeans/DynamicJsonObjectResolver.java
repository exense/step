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
package step.core.dynamicbeans;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import step.core.json.JsonProviderCache;
import step.expressions.ProtectedBinding;

public class DynamicJsonObjectResolver {
	
	DynamicJsonValueResolver valueResolver;
	
	public DynamicJsonObjectResolver(DynamicJsonValueResolver valueResolver) {
		super();
		this.valueResolver = valueResolver;
	}

	/**
	 * This method evaluates all groovy expression found in the provided object and return a resolved objects containing the evaluation results. This method does not have access to protected bindings
	 * @param o the json object to be evaluated recursively
	 * @param bindings the set of bindings (variables) that are used for the evaluation of groovy expressions contained in the provided object
	 * @return the resolved json object
	 */
	public JsonObject evaluate(JsonObject o, Map<String, Object> bindings) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		if(o!=null) {
			for(String key:o.keySet()) {
				JsonValue v = o.get(key);
				Object result = evaluateJsonValue(v, bindings);
				if(result instanceof JsonValue) {
					builder.add(key,(JsonValue)result);
				} else if(result instanceof Boolean) {
					builder.add(key,(Boolean)result);					
				} else if(result instanceof Integer) {
					builder.add(key,(Integer)result);
				} else if(result instanceof String) {
					builder.add(key,(String)result);
				} else {
					if(result != null)
						builder.add(key,(String)result.toString());
					else
						builder.add(key, ""); // a more restrictive try-catch of the NPE + throw "Value null for key={key}" might be more useful
				}
			}
		}
		return builder.build();
	}

	// Keep original methods for backward compatibility
	private Object evaluateJsonValue(JsonValue v, Map<String, Object> bindings) {
		DualValueEvaluationResult result = evaluateJsonValueDual(v, bindings, false);
		return result.normalValue;
	}

	public static class DualJsonResult {
		private final JsonObject normalResult;
		private final JsonObject obfuscatedResult;

		public DualJsonResult(JsonObject normalResult, JsonObject obfuscatedResult) {
			this.normalResult = normalResult;
			this.obfuscatedResult = obfuscatedResult;
		}

		public JsonObject getNormalResult() { return normalResult; }
		public JsonObject getObfuscatedResult() { return obfuscatedResult; }
	}


	/**
	 * This method is the only one that can evaluate groovy expression using protected binding, it will return a DualJsonResult containing the actual results and the obfuscated version
 	 * @param o the json object to be evaluated recursively
	 * @param bindings the set of bindings (variables) that are used for the evaluation of groovy expressions contained in the provided object
	 * @param canAccessProtectedValue whether access to protected value is granted
	 * @return the results of the evaluation as a DualJsonResult object containing both the actual results and the obfuscated one
	 */
	public DualJsonResult evaluateWithDualResults(JsonObject o, Map<String, Object> bindings, boolean canAccessProtectedValue) {
		DualJsonEvaluationResult result = evaluateInternal(o, bindings, canAccessProtectedValue);
		return new DualJsonResult(result.normalResult, result.obfuscatedResult);
	}

	// Internal helper class to track both results during recursion
	private static class DualJsonEvaluationResult {
		final JsonObject normalResult;
		final JsonObject obfuscatedResult;

		DualJsonEvaluationResult(JsonObject normalResult, JsonObject obfuscatedResult) {
			this.normalResult = normalResult;
			this.obfuscatedResult = obfuscatedResult;
		}
	}

	private DualJsonEvaluationResult evaluateInternal(JsonObject o, Map<String, Object> bindings,
													  boolean canAccessProtectedValue) {
		JsonObjectBuilder normalBuilder = JsonProviderCache.createObjectBuilder();
		JsonObjectBuilder obfuscatedBuilder = JsonProviderCache.createObjectBuilder();

		if(o != null) {
			for(String key : o.keySet()) {
				JsonValue v = o.get(key);

				DualValueEvaluationResult dualResult = evaluateJsonValueDual(v, bindings, canAccessProtectedValue);
				addToBuilder(normalBuilder, key, dualResult.normalValue);
				addToBuilder(obfuscatedBuilder, key, dualResult.obfuscatedValue);
			}
		}

		JsonObject normal = normalBuilder.build();
		JsonObject obfuscated = obfuscatedBuilder.build();

		return new DualJsonEvaluationResult(normal, obfuscated);
	}

	// Helper class for dual value evaluation
	private static class DualValueEvaluationResult {
		final Object normalValue;
		final Object obfuscatedValue;

		DualValueEvaluationResult(Object normalValue, Object obfuscatedValue) {
			this.normalValue = normalValue;
			this.obfuscatedValue = obfuscatedValue;
		}
	}

	private DualValueEvaluationResult evaluateJsonValueDual(JsonValue v, Map<String, Object> bindings, boolean canAccessProtectedValue) {
		if(v instanceof JsonObject) {
			JsonObject jsonObject = (JsonObject) v;
			if(jsonObject.containsKey("dynamic")) {
				Object normalEvaluate;
				Object obfuscatedEvaluate;
				Object result = valueResolver.evaluate(jsonObject, bindings, canAccessProtectedValue);
				if (result instanceof ProtectedBinding) {
					ProtectedBinding protectedBinding = (ProtectedBinding) result;
					normalEvaluate = protectedBinding.value;
					obfuscatedEvaluate = protectedBinding.obfuscatedValue;
				} else {
					normalEvaluate = result;
					obfuscatedEvaluate = result;
				}

				// Handle normal result
				Object normalFinalResult;
				if (normalEvaluate instanceof JsonObject) {
					DualJsonEvaluationResult subResult = evaluateInternal((JsonObject) normalEvaluate, bindings, canAccessProtectedValue);
					normalFinalResult = subResult.normalResult;
				} else if (normalEvaluate instanceof JsonArray) {
					JsonArray jsonArray = (JsonArray) normalEvaluate;
					DualArrayEvaluationResult arrayResult = evaluateJsonArrayDual(jsonArray, bindings, canAccessProtectedValue);
					normalFinalResult = arrayResult.normalResult;
				} else {
					normalFinalResult = normalEvaluate;
				}

				// Handle obfuscated result
				Object obfuscatedFinalResult;
				if (obfuscatedEvaluate instanceof JsonObject) {
					DualJsonEvaluationResult subResult = evaluateInternal((JsonObject) obfuscatedEvaluate, bindings, canAccessProtectedValue);
					obfuscatedFinalResult = subResult.obfuscatedResult;
				} else if (obfuscatedEvaluate instanceof JsonArray) {
					JsonArray jsonArray = (JsonArray) obfuscatedEvaluate;
					DualArrayEvaluationResult arrayResult = evaluateJsonArrayDual(jsonArray, bindings, canAccessProtectedValue);
					obfuscatedFinalResult = arrayResult.obfuscatedResult;
				} else {
					obfuscatedFinalResult = obfuscatedEvaluate;
				}

				return new DualValueEvaluationResult(normalFinalResult, obfuscatedFinalResult);
			} else {
				DualJsonEvaluationResult result = evaluateInternal(jsonObject, bindings, canAccessProtectedValue);
				return new DualValueEvaluationResult(result.normalResult, result.obfuscatedResult);
			}
		} else if (v instanceof JsonArray) {
			JsonArray jsonArray = (JsonArray) v;
			DualArrayEvaluationResult result = evaluateJsonArrayDual(jsonArray, bindings, canAccessProtectedValue);
			return new DualValueEvaluationResult(result.normalResult, result.obfuscatedResult);
		} else {
			// Primitive value - same for both normal and obfuscated
			return new DualValueEvaluationResult(v, v);
		}
	}

	// Helper class for dual array evaluation
	public static class DualArrayEvaluationResult {
		final JsonArray normalResult;
		final JsonArray obfuscatedResult;

		DualArrayEvaluationResult(JsonArray normalResult, JsonArray obfuscatedResult) {
			this.normalResult = normalResult;
			this.obfuscatedResult = obfuscatedResult;
		}
	}

	private DualArrayEvaluationResult evaluateJsonArrayDual(JsonArray jsonArray, Map<String, Object> bindings, boolean canAccessProtectedValue) {
		JsonArrayBuilder normalArrayBuilder = Json.createArrayBuilder();
		JsonArrayBuilder obfuscatedArrayBuilder = Json.createArrayBuilder();

        for (JsonValue jsonValue : jsonArray) {
            DualValueEvaluationResult dualResult = evaluateJsonValueDual(jsonValue, bindings, canAccessProtectedValue);

            addToArrayBuilder(normalArrayBuilder, dualResult.normalValue);
            addToArrayBuilder(obfuscatedArrayBuilder, dualResult.obfuscatedValue);
        }

		return new DualArrayEvaluationResult(normalArrayBuilder.build(), obfuscatedArrayBuilder.build());
	}

	// Helper method to add values to JsonObjectBuilder
	private void addToBuilder(JsonObjectBuilder builder, String key, Object result) {
		if(result instanceof JsonValue) {
			builder.add(key, (JsonValue)result);
		} else if(result instanceof Boolean) {
			builder.add(key, (Boolean)result);
		} else if(result instanceof Integer) {
			builder.add(key, (Integer)result);
		} else if(result instanceof String) {
			builder.add(key, (String)result);
		} else {
			if(result != null) {
				builder.add(key, result.toString());
			} else {
				builder.add(key, "");
			}
		}
	}

	// Helper method to add values to JsonArrayBuilder
	private void addToArrayBuilder(JsonArrayBuilder builder, Object o) {
		if(o instanceof JsonValue) {
			builder.add((JsonValue)o);
		} else if(o instanceof Boolean) {
			builder.add((Boolean)o);
		} else if(o instanceof Integer) {
			builder.add((Integer)o);
		} else if(o instanceof String) {
			builder.add((String)o);
		} else {
			builder.add(o.toString());
		}
	}

}
