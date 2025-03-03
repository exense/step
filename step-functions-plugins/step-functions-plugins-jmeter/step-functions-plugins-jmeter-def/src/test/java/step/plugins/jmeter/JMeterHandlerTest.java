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
package step.plugins.jmeter;

import ch.exense.commons.app.Configuration;
import jakarta.json.JsonObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.io.Output;
import step.functions.runner.FunctionRunner;
import step.functions.runner.FunctionRunner.Context;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.io.Attachment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Category(step.junit.categories.LocalJMeter.class)
public class JMeterHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(JMeterHandlerTest.class);
    private String jMeterHome;

    @Before
    public void before() {
        jMeterHome = System.getenv().get(JMeterFunctionTypeLocalPlugin.JMETER_HOME_ENV_VAR);
    }

    @Test
    public void test1() {
        assertJMeterHome();
        JMeterFunction f = buildTestFunction();
        Output<JsonObject> output = run(f, "{\"url\":\"www.exense.ch\"}", false);
        Assert.assertNull(output.getError());
        Assert.assertNotNull(output.getPayload().get("samples"));
    }

    @Test
    public void testDebug() {
        assertJMeterHome();
        JMeterFunction f = buildTestFunction();
        Output<JsonObject> output = run(f, "{\"url\":\"www.exense.ch\"}", true);
        Assert.assertNull(output.getError());
        Assert.assertNotNull(output.getPayload().get("samples"));
        Assert.assertEquals(1, output.getAttachments().size());
        Attachment attachment = output.getAttachments().get(0);
        Assert.assertEquals("log.txt", attachment.getName());
    }

    @Test
    public void testError() {
        assertJMeterHome();
        JMeterFunction f = buildTestFunction();
        Output<JsonObject> output = run(f, "{\"url\":\"www.exense2.ch\"}", false);
        Assert.assertEquals("The following samples returned errors (error count in parentheses): HTTP Request (1)", output.getError().getMsg());
        Assert.assertNotNull(output.getPayload().get("samples"));
        Assert.assertEquals(1, output.getAttachments().size());
        Attachment attachment = output.getAttachments().get(0);
        Assert.assertEquals("log.txt", attachment.getName());
    }

    @Test
    public void testProperties() {
        assertJMeterHome();
        JMeterFunction f = buildTestFunction();
        Output<JsonObject> output = run(f, "{}", Map.of("url", "www.exense.ch"));
        Assert.assertNull(output.getError());
        Assert.assertNotNull(output.getPayload().get("samples"));

        // Assert precedence of input
        output = run(f, "{\"url\":\"www.exense.ch\"}", Map.of("url", "wrong url"));
        Assert.assertNull(output.getError());
        Assert.assertNotNull(output.getPayload().get("samples"));
    }

    private void assertJMeterHome() {
        if (jMeterHome == null) {
            Assert.fail("JMeter home variable is not defined. If you want to skip JMeter tests please active the 'SkipJMeterTests' maven profile");
        }
    }

    private Output<JsonObject> run(JMeterFunction f, String inputJson, boolean debug) {
        Map<String, String> properties = (debug) ? Map.of("debug", "true") : new HashMap<>();
        return run(f, inputJson, properties);
    }

    private Output<JsonObject> run(JMeterFunction f, String inputJson, Map<String, String> properties) {
        Configuration configuration = new Configuration();
        configuration.putProperty(JMeterFunctionType.JMETER_HOME_CONFIG_PROPERTY, jMeterHome);

        try (Context context = FunctionRunner.getContext(configuration, new JMeterFunctionType(configuration), properties)) {
            return context.run(f, inputJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JMeterFunction buildTestFunction() {
        File file = ResourceExtractor.extractResource(this.getClass().getClassLoader(), "scripts/Demo_JMeter.jmx");
        JMeterFunction f = new JMeterFunction();
        f.setJmeterTestplan(new DynamicValue<>(file.getAbsolutePath()));
        f.setId(new ObjectId());
        Map<String, String> attributes = new HashMap<>();
        attributes.put(AbstractOrganizableObject.NAME, "medor");
        f.setAttributes(attributes);
        return f;
    }
}
