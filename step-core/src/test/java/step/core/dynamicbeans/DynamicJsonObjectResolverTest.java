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

import java.io.StringReader;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

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
        Assert.assertTrue(output.getBoolean("testBoolean"));
        Assert.assertEquals(10, output.getInt("testInteger"));
        Assert.assertEquals("test", ((JsonObject) output.getJsonArray("testArray").get(0)).getString("testString"));
        Assert.assertEquals("test", output.getJsonObject("testRecursive2").getString("testString"));
    }

    // Interpolation of plain (non dynamic) values

    @Test
    public void testPlainValueIsInterpolated() {
        JsonObject output = resolver.evaluate(json("{'in':{'dynamic':false,'value':'Hello ${name}'}}"), Map.of("name", "John"));
        Assert.assertEquals("Hello John", output.getString("in"));
    }

    @Test
    public void testPlainValueWithoutExpressionIsUnchanged() {
        JsonObject output = resolver.evaluate(json("{'in':{'dynamic':false,'value':'Hello world'}}"), Map.of("name", "John"));
        Assert.assertEquals("Hello world", output.getString("in"));
    }

    @Test
    public void testEscapedPlaceholder() {
        JsonObject output = resolver.evaluate(json("{'in':{'dynamic':false,'value':'$${name}'}}"), Map.of("name", "John"));
        Assert.assertEquals("${name}", output.getString("in"));
    }

    @Test
    public void testBackslashesAndQuotesArePreserved() {
        JsonObject output = resolver.evaluate(json("{'in':{'dynamic':false,'value':'C:\\\\temp\\\\${name}'}}"), Map.of("name", "John"));
        Assert.assertEquals("C:\\temp\\John", output.getString("in"));
    }

    @Test
    public void testNonStringPlainValuesAreNotInterpolated() {
        JsonObject output = resolver.evaluate(json("{'i':{'dynamic':false,'value':5},'b':{'dynamic':false,'value':true}}"), Map.of());
        Assert.assertEquals(5, output.getInt("i"));
        Assert.assertTrue(output.getBoolean("b"));
    }

    @Test
    public void testMalformedExpression() {
        RuntimeException e = Assert.assertThrows(RuntimeException.class,
            () -> resolver.evaluate(json("{'in':{'dynamic':false,'value':'Hello ${name'}}"), Map.of("name", "John")));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no '}' found"));
    }

    @Test
    public void testNestedPlainValuesAreInterpolated() {
        // The leaves of a nested structure are interpolated by the resolver of that structure
        JsonObject output = resolver.evaluate(json("{'outer':{'dynamic':false,'value':'plain'},'in':{'dynamic':false,'value':'Hello ${name}'}}"), Map.of("name", "John"));
        Assert.assertEquals("plain", output.getString("outer"));
        Assert.assertEquals("Hello John", output.getString("in"));
    }

    @Test
    public void testProtectedValueInterpolation() {
        JsonObject input = json("{'in':{'dynamic':false,'value':'pwd=${secret}'}}");
        Map<String, Object> bindings = Map.of("secret", new ProtectedVariable("secret", "myPassword"));

        DynamicJsonObjectResolver.DualJsonResult result = resolver.evaluateWithDualResults(input, bindings, true);
        Assert.assertEquals("pwd=myPassword", result.getNormalResult().getString("in"));
        Assert.assertEquals("pwd=***secret***", result.getObfuscatedResult().getString("in"));
    }

    @Test
    public void testProtectedValueIsNotLeakedWithoutProtectedAccess() {
        JsonObject input = json("{'in':{'dynamic':false,'value':'pwd=${secret}'}}");
        Map<String, Object> bindings = Map.of("secret", new ProtectedVariable("secret", "myPassword"));

        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> resolver.evaluate(input, bindings));
        Assert.assertFalse(e.getMessage(), e.getMessage().contains("myPassword"));
    }

    private static JsonObject json(String json) {
        return Json.createReader(new StringReader(json.replace('\'', '"'))).readObject();
    }
}
