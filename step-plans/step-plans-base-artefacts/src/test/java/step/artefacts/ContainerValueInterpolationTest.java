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
package step.artefacts;

import java.io.StringReader;
import java.util.Map;

import jakarta.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.json.JsonProviderCache;
import step.expressions.ExpressionHandler;

/**
 * The values holding a structured document must not be interpolated as a whole. Their expressions are resolved by
 * the resolver of that document, once it has been parsed
 */
public class ContainerValueInterpolationTest {

    private static final Map<String, Object> BINDINGS = Map.of("host", "myhost", "quoted", "He said \"hi\"");

    private final DynamicBeanResolver beanResolver = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
    private final DynamicJsonObjectResolver jsonResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()));

    /**
     * Regression test: interpolating the keyword input JSON as a whole resolved the expressions before the document
     * was parsed. Any resolved value containing a quote, a backslash or a line break corrupted the document
     */
    @Test
    public void testKeywordInputJsonIsNotInterpolatedAsAWhole() {
        String inputs = "{\"url\":{\"dynamic\":false,\"value\":\"http://${host}\"}," +
            "\"who\":{\"dynamic\":false,\"value\":\"${quoted}\"}}";
        CallFunction callFunction = new CallFunction();
        callFunction.setArgument(new DynamicValue<>(inputs));

        beanResolver.evaluate(callFunction, BINDINGS);

        // The container is left untouched and remains parseable
        Assert.assertEquals(inputs, callFunction.getArgument().get());

        // The expressions are resolved when the parsed document is evaluated
        JsonObject resolved = jsonResolver.evaluate(parse(callFunction.getArgument().get()), BINDINGS);
        Assert.assertEquals("http://myhost", resolved.getString("url"));
        Assert.assertEquals("He said \"hi\"", resolved.getString("who"));
    }

    @Test
    public void testSelectionCriteriaJsonIsNotInterpolatedAsAWhole() {
        String criteria = "{\"name\":{\"dynamic\":false,\"value\":\"keyword-${host}\"}}";
        CallFunction callFunction = new CallFunction();
        callFunction.setFunction(new DynamicValue<>(criteria));

        beanResolver.evaluate(callFunction, BINDINGS);

        Assert.assertEquals(criteria, callFunction.getFunction().get());
        Assert.assertEquals("keyword-myhost", jsonResolver.evaluate(parse(callFunction.getFunction().get()), BINDINGS).getString("name"));
    }

    @Test
    public void testCallPlanAndReturnContainersAreNotInterpolated() {
        String document = "{\"a\":{\"dynamic\":false,\"value\":\"${host}\"}}";

        CallPlan callPlan = new CallPlan();
        callPlan.setInput(new DynamicValue<>(document));
        callPlan.setSelectionAttributes(new DynamicValue<>(document));
        beanResolver.evaluate(callPlan, BINDINGS);
        Assert.assertEquals(document, callPlan.getInput().get());
        Assert.assertEquals(document, callPlan.getSelectionAttributes().get());

        Return returnArtefact = new Return();
        returnArtefact.setOutput(new DynamicValue<>(document));
        beanResolver.evaluate(returnArtefact, BINDINGS);
        Assert.assertEquals(document, returnArtefact.getOutput().get());
    }

    @Test
    public void testTokenSelectionCriteriaIsNotInterpolated() {
        String token = "{\"os\":{\"dynamic\":false,\"value\":\"${host}\"}}";
        CallFunction callFunction = new CallFunction();
        callFunction.setToken(new DynamicValue<>(token));
        beanResolver.evaluate(callFunction, BINDINGS);
        Assert.assertEquals(token, callFunction.getToken().get());
    }

    /**
     * The exclusion only concerns plain values. A container defined as an expression is still evaluated
     */
    @Test
    public void testContainerDefinedAsExpressionIsStillEvaluated() {
        CallFunction callFunction = new CallFunction();
        callFunction.setArgument(new DynamicValue<>("'{\"url\":\"' + host + '\"}'", ""));

        beanResolver.evaluate(callFunction, BINDINGS);

        Assert.assertEquals("{\"url\":\"myhost\"}", callFunction.getArgument().get());
    }

    /**
     * The other values of the same artefact are interpolated as usual
     */
    @Test
    public void testNonContainerValuesOfTheSameArtefactAreInterpolated() {
        CallFunction callFunction = new CallFunction();
        callFunction.setResultMap(new DynamicValue<>("output_${host}"));

        beanResolver.evaluate(callFunction, BINDINGS);

        Assert.assertEquals("output_myhost", callFunction.getResultMap().get());
    }

    private static JsonObject parse(String json) {
        return JsonProviderCache.createReader(new StringReader(json)).readObject();
    }
}
