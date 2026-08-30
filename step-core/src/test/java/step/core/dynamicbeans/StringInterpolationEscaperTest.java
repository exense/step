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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

public class StringInterpolationEscaperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testPlainValuesAreEscaped() {
        Map<String, Object> document = document("{'root':{'text':{'dynamic':false,'value':'Hello ${name}'}}}");
        Assert.assertTrue(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("Hello $${name}", valueAt(document, "root", "text"));
    }

    @Test
    public void testValuesWithoutSignificantDollarAreLeftUntouched() {
        Map<String, Object> document = document("{'root':{'text':{'dynamic':false,'value':'Price: $5'}}}");
        Assert.assertFalse(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("Price: $5", valueAt(document, "root", "text"));
    }

    /**
     * Only ${ became significant, so a value containing a doubled dollar is left alone. This keeps the migration
     * away from shell scripts, passwords and the like
     */
    @Test
    public void testDoubleDollarIsNotEscaped() {
        Map<String, Object> document = document("{'root':{'text':{'dynamic':false,'value':'pid $$ of $$'}}}");
        Assert.assertFalse(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("pid $$ of $$", valueAt(document, "root", "text"));
    }

    @Test
    public void testExpressionsAreNotEscaped() {
        Map<String, Object> expression = new HashMap<>();
        expression.put("dynamic", true);
        expression.put("expression", "\"Hello ${name}\"");
        Map<String, Object> document = new HashMap<>(Map.of("text", expression));

        Assert.assertFalse(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("\"Hello ${name}\"", expression.get("expression"));
    }

    @Test
    public void testNestedChildrenAndListsAreWalked() {
        Map<String, Object> document = document("{'root':{'children':[" +
            "{'text':{'dynamic':false,'value':'a ${x}'}}," +
            "{'children':[{'text':{'dynamic':false,'value':'b ${y}'}}]}]}}");
        Assert.assertTrue(StringInterpolationEscaper.escapeDocument(document));

        List<?> children = (List<?>) ((Map<?, ?>) document.get("root")).get("children");
        Assert.assertEquals("a $${x}", value((Map<?, ?>) ((Map<?, ?>) children.get(0)).get("text")));
        List<?> nested = (List<?>) ((Map<?, ?>) children.get(1)).get("children");
        Assert.assertEquals("b $${y}", value((Map<?, ?>) ((Map<?, ?>) nested.get(0)).get("text")));
    }

    /**
     * The keyword input JSON isn't interpolated as a whole, so the container must not be escaped. The plain values
     * it contains are interpolated and must be
     */
    @Test
    public void testContainerFieldsAreEscapedInDepth() throws Exception {
        Map<String, Object> document = documentWithArgument(
            "{\"url\":{\"dynamic\":false,\"value\":\"http://${host}\"},\"plain\":{\"dynamic\":false,\"value\":\"none\"}}");

        Assert.assertTrue(StringInterpolationEscaper.escapeDocument(document));

        Map<String, Object> inputs = MAPPER.readValue(valueAt(document, "root", "argument"), new TypeReference<>() {
        });
        Assert.assertEquals("http://$${host}", value((Map<?, ?>) inputs.get("url")));
        Assert.assertEquals("none", value((Map<?, ?>) inputs.get("plain")));
    }

    /**
     * An input of a keyword may legitimately be named like a container field. The exclusion must not cascade
     */
    @Test
    public void testContainerExclusionDoesNotCascadeToInputNames() throws Exception {
        Map<String, Object> document = documentWithArgument("{\"input\":{\"dynamic\":false,\"value\":\"${x}\"}}");

        Assert.assertTrue(StringInterpolationEscaper.escapeDocument(document));

        Map<String, Object> inputs = MAPPER.readValue(valueAt(document, "root", "argument"), new TypeReference<>() {
        });
        Assert.assertEquals("$${x}", value((Map<?, ?>) inputs.get("input")));
    }

    private static Map<String, Object> documentWithArgument(String argumentJson) {
        Map<String, Object> argument = new HashMap<>();
        argument.put("dynamic", false);
        argument.put("value", argumentJson);
        return new HashMap<>(Map.of("root", new HashMap<>(Map.of("argument", argument))));
    }

    @Test
    public void testContainerHoldingSomethingElseThanJsonIsLeftUntouched() {
        Map<String, Object> document = document("{'root':{'argument':{'dynamic':false,'value':'not json ${x}'}}}");
        Assert.assertFalse(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("not json ${x}", valueAt(document, "root", "argument"));
    }

    @Test
    public void testUnrelatedMapsWithAValueKeyAreNotMistakenForDynamicValues() {
        Map<String, Object> document = document("{'root':{'someMap':{'value':'${x}','other':'y'}}}");
        Assert.assertFalse(StringInterpolationEscaper.escapeDocument(document));
        Assert.assertEquals("${x}", ((Map<?, ?>) ((Map<?, ?>) document.get("root")).get("someMap")).get("value"));
    }

    /**
     * The migrated values must be interpolated back to what they were before the migration
     */
    @Test
    public void testMigratedValuesResolveToTheOriginalLiteral() {
        String original = "template ${JOB_NAME} with $$ and C:\\logs\\file.txt";
        Map<String, Object> document = new HashMap<>(Map.of("text",
            new HashMap<String, Object>(Map.of("dynamic", false, "value", original))));

        Assert.assertTrue(StringInterpolationEscaper.escapeDocument(document));

        DynamicValue<String> migrated = new DynamicValue<>(valueAt(document, "text"));
        new DynamicValueResolver(new step.expressions.ExpressionHandler()).evaluate(migrated, Map.of());
        Assert.assertEquals(original, migrated.get());
    }

    private static Map<String, Object> document(String json) {
        try {
            return MAPPER.readValue(json.replace('\'', '"'), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String valueAt(Map<String, Object> document, String... path) {
        Object current = document;
        for (String key : path) {
            current = ((Map<?, ?>) current).get(key);
        }
        return value((Map<?, ?>) current);
    }

    private static String value(Map<?, ?> dynamicValue) {
        return (String) dynamicValue.get("value");
    }
}
