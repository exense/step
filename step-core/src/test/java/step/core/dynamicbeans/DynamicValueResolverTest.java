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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

public class DynamicValueResolverTest {

    private static final Map<String, Object> NAME_BINDING = Map.of("name", "John");

    @Test
    public void testString() {
        DynamicValue<String> v1 = new DynamicValue<>("'test'", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals("test", v1.get());
    }

    @Test
    public void testString2() {
        DynamicValue<String> v1 = new DynamicValue<>("\"test\"", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals("test", v1.get());
    }

    @Test
    public void testGString() {
        DynamicValue<String> v1 = new DynamicValue<>("\"te${'s'}t\"", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals("test", v1.get());
    }

    @Test
    public void testGStringVariables() {
        DynamicValue<String> v1 = new DynamicValue<>("\"t${var}t\"", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("var", "es");
        resolver.evaluate(v1, bindings);
        Assert.assertEquals("test", v1.get());
    }

    @Test
    public void testInteger() {
        DynamicValue<Integer> v1 = new DynamicValue<>("1", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals(1, (int) v1.get());
    }

    @Test
    public void testBoolean() {
        DynamicValue<Boolean> v1 = new DynamicValue<>("true", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals(true, (boolean) v1.get());
    }

    @Test
    public void testJSONObject() {
        DynamicValue<JSONObject> v1 = new DynamicValue<>("new org.json.JSONObject(\"{'key1':'test'}\")", "");
        DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
        resolver.evaluate(v1, null);
        Assert.assertEquals(new JSONObject("{'key1':'test'}").get("key1"), v1.get().get("key1"));
    }

    // Interpolation of plain (non dynamic) string values

    @Test
    public void testInterpolatedString() {
        Assert.assertEquals("Hello John", interpolate("Hello ${name}", NAME_BINDING));
    }

    @Test
    public void testInterpolatedStringMultiplePlaceholders() {
        Assert.assertEquals("John reads a book", interpolate("${name} reads a ${item}", Map.of("name", "John", "item", "book")));
    }

    @Test
    public void testValueWithoutPlaceholderIsReturnedAsIs() {
        DynamicValue<String> v1 = new DynamicValue<>("Hello world");
        newResolver().evaluate(v1, NAME_BINDING);
        Assert.assertEquals("Hello world", v1.get());
        // No evaluation took place at all
        Assert.assertNull(v1.evalutationResult);
        Assert.assertFalse(v1.interpolatedLiteral);
    }

    /**
     * A value which doesn't contain ${ is used as is and is never even parsed, whatever dollars it contains
     */
    @Test
    public void testValueWithoutExpressionPrefixIsReturnedAsIs() {
        for (String literal : List.of("Price: $5 and $name", "pid $$", "a$$b", "$", "100% $$ done")) {
            DynamicValue<String> v1 = new DynamicValue<>(literal);
            newResolver().evaluate(v1, NAME_BINDING);
            Assert.assertEquals(literal, v1.get());
            Assert.assertNull("No evaluation should have taken place for <" + literal + ">", v1.evalutationResult);
            Assert.assertFalse(v1.interpolatedLiteral);
        }
    }

    @Test
    public void testEscapedPlaceholder() {
        // The escape sequence is resolved without any evaluation taking place
        Assert.assertEquals("${name}", interpolate("$${name}", NAME_BINDING));
        Assert.assertEquals("$${name}", interpolate("$$${name}", NAME_BINDING));
        // Only ${ is significant, so a value without it is never altered
        Assert.assertEquals("a$$b", interpolate("a$$b", NAME_BINDING));
        // A literal $ in front of an expression is written with the expression itself
        Assert.assertEquals("$John", interpolate("${'$'}${name}", NAME_BINDING));
    }

    /**
     * Regression test: the literal parts of the value must never reach the groovy lexer, which would
     * otherwise interpret the backslashes and be terminated by the double quotes
     */
    @Test
    public void testBackslashesAndQuotesArePreserved() {
        Assert.assertEquals("{\"path\":\"C:\\temp\\John\"}", interpolate("{\"path\":\"C:\\temp\\${name}\"}", NAME_BINDING));
        Assert.assertEquals("\\d+ John", interpolate("\\d+ ${name}", NAME_BINDING));
        Assert.assertEquals("He said \"hi\" to John", interpolate("He said \"hi\" to ${name}", NAME_BINDING));
    }

    @Test
    public void testMultilineValue() {
        Assert.assertEquals("line1\nline2 John", interpolate("line1\nline2 ${name}", NAME_BINDING));
    }

    @Test
    public void testExpressionResultTypes() {
        Assert.assertEquals("2", interpolate("${1+1}", null));
        Assert.assertEquals("null", interpolate("${null}", null));
        Assert.assertEquals("[1, 2]", interpolate("${[1,2]}", null));
        Assert.assertEquals("true", interpolate("${1<2}", null));
    }

    @Test
    public void testNonStringValuesAreNotInterpolated() {
        DynamicValue<Integer> v1 = new DynamicValue<>(5);
        newResolver().evaluate(v1, NAME_BINDING);
        Assert.assertEquals(5, (int) v1.get());
        Assert.assertNull(v1.evalutationResult);
    }

    @Test
    public void testNullValue() {
        DynamicValue<String> v1 = new DynamicValue<>((String) null);
        newResolver().evaluate(v1, NAME_BINDING);
        Assert.assertNull(v1.get());
    }

    @Test
    public void testMissingBinding() {
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${missingVariable}");
        newResolver().evaluate(v1, NAME_BINDING);
        RuntimeException e = Assert.assertThrows(RuntimeException.class, v1::get);
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("missingVariable"));
    }

    @Test
    public void testMalformedExpression() {
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${name");
        newResolver().evaluate(v1, NAME_BINDING);
        RuntimeException e = Assert.assertThrows(RuntimeException.class, v1::get);
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no '}' found"));
    }

    @Test
    public void testInterpolationOfPlainValueDisabled() {
        // Used for the values holding a structured document, see NoStringInterpolation
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${name}");
        newResolver().evaluate(v1, NAME_BINDING, false);
        Assert.assertEquals("Hello ${name}", v1.get());
        Assert.assertNull(v1.evalutationResult);
    }

    @Test
    public void testInterpolationOfPlainValueDisabledDoesNotAffectDynamicValues() {
        DynamicValue<String> v1 = new DynamicValue<>("\"Hello ${name}\"", "");
        newResolver().evaluate(v1, NAME_BINDING, false);
        Assert.assertEquals("Hello John", v1.get());
    }

    /**
     * Regression test: the state left by a previous interpolation must not be returned after the value changed
     */
    @Test
    public void testReevaluationAfterValueChange() {
        DynamicValueResolver resolver = newResolver();
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${name}");
        resolver.evaluate(v1, NAME_BINDING);
        Assert.assertEquals("Hello John", v1.get());

        v1.setValue("Hello world");
        Assert.assertEquals("Hello world", v1.get());
        resolver.evaluate(v1, NAME_BINDING);
        Assert.assertEquals("Hello world", v1.get());
        Assert.assertNull(v1.evalutationResult);
    }

    @Test
    public void testReevaluationWithNewBindings() {
        DynamicValueResolver resolver = newResolver();
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${name}");
        resolver.evaluate(v1, NAME_BINDING);
        Assert.assertEquals("Hello John", v1.get());
        resolver.evaluate(v1, Map.of("name", "Jane"));
        Assert.assertEquals("Hello Jane", v1.get());
    }

    @Test
    public void testCloneDiscardsTheInterpolationResult() {
        DynamicValue<String> v1 = new DynamicValue<>("Hello ${name}");
        newResolver().evaluate(v1, NAME_BINDING);
        Assert.assertEquals("Hello John", v1.get());

        DynamicValue<String> clone = v1.cloneValue();
        // The clone hasn't been evaluated yet and returns the raw literal
        Assert.assertEquals("Hello ${name}", clone.get());
        newResolver().evaluate(clone, Map.of("name", "Jane"));
        Assert.assertEquals("Hello Jane", clone.get());
    }

    // Protected values

    @Test
    public void testProtectedDynamicValueInterpolation() {
        ProtectedDynamicValue<String> v1 = new ProtectedDynamicValue<>("password=${secret}");
        newResolver().evaluate(v1, Map.of("secret", new ProtectedVariable("secret", "myPassword")));
        // Callers with protected access get the clear value
        Assert.assertEquals("password=myPassword", v1.get());
        // while the result value, which is the one used for reporting, remains obfuscated
        Assert.assertEquals("password=***secret***", v1.evalutationResult.getResultValue());
    }

    @Test
    public void testProtectedValueIsNotLeakedWithoutProtectedAccess() {
        DynamicValue<String> v1 = new DynamicValue<>("password=${secret}");
        newResolver().evaluate(v1, Map.of("secret", new ProtectedVariable("secret", "myPassword")));
        // Without protected access the evaluation of a protected binding fails
        RuntimeException e = Assert.assertThrows(RuntimeException.class, v1::get);
        Assert.assertFalse(e.getMessage(), e.getMessage().contains("myPassword"));
    }

    private static DynamicValueResolver newResolver() {
        return new DynamicValueResolver(new ExpressionHandler());
    }

    private static String interpolate(String value, Map<String, Object> bindings) {
        DynamicValue<String> dynamicValue = new DynamicValue<>(value);
        newResolver().evaluate(dynamicValue, bindings);
        return dynamicValue.get();
    }
}
