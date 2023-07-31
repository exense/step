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
package step.artefacts.handlers;

import jakarta.json.JsonObject;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import jakarta.json.JsonValue;
import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.handlers.asserts.*;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

import java.util.HashMap;
import java.util.Map;

public class AssertHandler extends ArtefactHandler<Assert, AssertReportNode> {

	private final Map<AssertOperator, AssertOperatorHandler> operatorHandlers;

	public AssertHandler() {
		this.operatorHandlers = new HashMap<>();
		this.operatorHandlers.put(AssertOperator.EQUALS, new EqualsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.CONTAINS, new ContainsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.BEGINS_WITH, new BeginsWithOperatorHandler());
		this.operatorHandlers.put(AssertOperator.ENDS_WITH, new EndsWithOperatorHandler());
		this.operatorHandlers.put(AssertOperator.MATCHES, new MatchesOperatorHandler());
		this.operatorHandlers.put(AssertOperator.GREATER_THAN, new GreaterThanOperatorHandler());
		this.operatorHandlers.put(AssertOperator.GREATER_THAN_OR_EQUALS, new GreaterThanOrEqualsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.LESS_THAN, new LessThanOperatorHandler());
		this.operatorHandlers.put(AssertOperator.LESS_THAN_OR_EQUALS, new LessThanOrEqualsOperatorHandler());
		this.operatorHandlers.put(AssertOperator.IS_NULL, new IsNullOperatorHandler());
	}

	@Override
	protected void createReportSkeleton_(AssertReportNode parentNode, Assert artefact) {

	}

	@Override
	protected void execute_(AssertReportNode node, Assert artefact) {
		CallFunctionReportNode callFunctionReport = (CallFunctionReportNode) context.getVariablesManager().getVariable("callReport");
		if (callFunctionReport == null) {
			// TODO externalize error messages and use error codes at this place.
			throw new RuntimeException("Keyword report unreachable. Asserts should be wrapped in Keyword nodes in the test plan.");
		}
		if (callFunctionReport.getStatus() == ReportNodeStatus.PASSED) {
			JsonObject outputJson = callFunctionReport.getOutputObject();
			String key = artefact.getActual().get();
			AssertOperator operator = artefact.getOperator();
			node.setKey(key);

			// Expected value has a String generic type, but in fact it can be resolved to Boolean or Number
			// so here we use Object type for expected value
			Object expectedValue = artefact.getExpected().get();

			ValueResolvingResult valueResolvingResult = resolveValue(outputJson, key);

			boolean passed = false;
			if (valueResolvingResult.actualResolved) {
				node.setActual(valueToString(valueResolvingResult.actual));
				node.setExpected(expectedValue);

				//boolean negate = artefact.isNegate();
				boolean negate = artefact.getDoNegate().get();

				AssertResult assertResult = applyOperator(key, valueResolvingResult, expectedValue, negate, operator);

				node.setDescription(assertResult.getDescription());
				node.setMessage(assertResult.getMessage());
				node.setStatus(assertResult.isPassed() ? ReportNodeStatus.PASSED : ReportNodeStatus.FAILED);

				passed = assertResult.isPassed();
			} else {
				node.setMessage(valueResolvingResult.message);
				node.setStatus(ReportNodeStatus.FAILED);
			}

			if (!passed) {
				String customErrorMessage = artefact.getCustomErrorMessage().get();
				if (customErrorMessage != null && !customErrorMessage.isEmpty()) {
					node.setMessage(customErrorMessage);
				}
			}
		} else {
			node.setStatus(ReportNodeStatus.NORUN);
		}
	}

	private ValueResolvingResult resolveValue(JsonObject outputJson, String key) {
		if (key.startsWith("$")) {
			return resolveJsonPathValue(outputJson, key);
		} else {
			return resolveSimpleValue(outputJson, key);
		}
	}

	private ValueResolvingResult resolveJsonPathValue(JsonObject outputJson, String key) {
		ValueResolvingResult valueResolvingResult = new ValueResolvingResult();
		try {
			valueResolvingResult.actual = JsonPath.parse(outputJson.toString()).read(key);
			valueResolvingResult.actualResolved = true;
		} catch (PathNotFoundException e) {
			// the attribute is missing (but we mark the value as resolved because some operators like 'notNull' support the missing values as nulls)
			valueResolvingResult.actual = null;
			valueResolvingResult.message = e.getMessage();
			valueResolvingResult.actualResolved = true;
		}
		valueResolvingResult.type = ValueType.JSON_PATH;
		return valueResolvingResult;
	}

	private ValueResolvingResult resolveSimpleValue(JsonObject outputJson, String key) {
		ValueResolvingResult result = new ValueResolvingResult();
		JsonValue jsonValue = outputJson.get(key);
		if (jsonValue == null) {
			// the attribute is missing (but we mark the value as resolved because some operators like 'notNull' support the missing values as nulls)
			result.actual = null;
			result.actualResolved = true;
		} else if (jsonValue.getValueType() == JsonValue.ValueType.NULL) {
			result.actual = null;
			result.actualResolved = true;
		} else if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
			result.actual = outputJson.getString(key);
			result.actualResolved = true;
		} else if (jsonValue.getValueType() == JsonValue.ValueType.NUMBER) {
			result.actual = outputJson.getJsonNumber(key).numberValue();
			result.actualResolved = true;
		} else if (jsonValue.getValueType() == JsonValue.ValueType.FALSE || jsonValue.getValueType() == JsonValue.ValueType.TRUE) {
			result.actual = outputJson.getBoolean(key);
			result.actualResolved = true;
		} else {
			result.message = "Type of " + key + " (" + jsonValue.getValueType() + ") is not supported";
			result.actualResolved = false;
		}

		result.type = ValueType.SIMPLE;
		return result;
	}

	private AssertResult applyOperator(String key, ValueResolvingResult valueResolvingResult, Object expectedValue, boolean negate, AssertOperator operator) {
		Object actual = valueResolvingResult.actual;
		ValueType type = valueResolvingResult.type;

		AssertOperatorHandler handler = getOperatorHandler(operator);

		if (!handler.isActualValueSupported(actual)) {
			String message;

			if (valueResolvingResult.message != null && !valueResolvingResult.message.isEmpty()) {
				// in some cases (like in case of missing attributes) the error message is already prepared during value resolving
				message = valueResolvingResult.message;
			} else {

				if (actual == null) {
					// user-friendly message for null-value
					message = "Unable to execute assertion. The keyword output doesn't contain the attribute '" + key + "'";
				} else if (type == ValueType.JSON_PATH) {
					// json path value
					message = "The json path '" + key + "' returns an object of type "
							+ actual.getClass().getSimpleName() + " which is not supported for operator " + operator.name();
				} else {
					// simple value
					message = "Type of " + key + " ("
							+ actual.getClass().getSimpleName()
							+ ") is not supported for operator " + operator;
				}
			}
			return createFailedAssertResult(message);
		}

		if (!handler.isExpectedValueSupported(expectedValue)) {
			String message;
			if (expectedValue == null || (expectedValue instanceof String && ((String) expectedValue).isEmpty())) {
				// user-friendly message for null-value
				message = "Unable to execute assertion. The expected value is not defined for the attribute '" + key + "'";
			} else {
				message = "Type of expected value (" + expectedValue.getClass().getSimpleName() + ") of " + key +
						" is not supported for operator " + operator;
			}
			return createFailedAssertResult(message);
		}

		return handler.apply(key, actual, expectedValue, negate);
	}

	public static final String valueToString(Object value) {
		return value == null ? null : value.toString();
	}

	private AssertResult createFailedAssertResult(String message){
		AssertResult result = new AssertResult();
		result.setDescription(null);
		result.setMessage(message);
		result.setPassed(false);
		return result;
	}

	@Override
	public AssertReportNode createReportNode_(ReportNode parentNode, Assert artefact) {
		return new AssertReportNode();
	}

	private AssertOperatorHandler getOperatorHandler(AssertOperator operator) {
		AssertOperatorHandler handler = operatorHandlers.get(operator);
		if (handler == null) {
			throw new IllegalStateException("Handler is not defined for operator " + operator);
		}
		return handler;
	}

	private static class ValueResolvingResult {
		private Object actual = null;
		private boolean actualResolved = false;
		private String message = null;
		private ValueType type = null;
	}

	private enum ValueType {
		SIMPLE,
		JSON_PATH
	}

}
