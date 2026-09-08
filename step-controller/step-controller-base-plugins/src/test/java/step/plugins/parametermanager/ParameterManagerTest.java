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
package step.plugins.parametermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.test.categories.PerformanceTest;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.encryption.EncryptionManager;
import step.core.collections.mongodb.MongoDBCollectionFactory;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.parameter.Parameter;
import step.commons.activation.Expression;
import step.core.accessors.InMemoryAccessor;
import step.core.objectenricher.ObjectPredicate;
import step.parameter.ParameterManager;

public class ParameterManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(ParameterManagerTest.class);

    private final DynamicBeanResolver resolver = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));

    private static final String SECRET = "MySecretValue";

    /**
     * Trivial encryption manager prefixing the value, enough to tell an encrypted value from a clear one
     */
    private static final EncryptionManager ENCRYPTION_MANAGER = new EncryptionManager() {
        @Override
        public String encrypt(String value) {
            return "###" + value;
        }

        @Override
        public String decrypt(String encryptedValue) {
            return encryptedValue.replaceFirst("###", "");
        }

        @Override
        public boolean isKeyPairChanged() {
            return false;
        }

        @Override
        public boolean isFirstStart() {
            return false;
        }
    };

    private static Map<String, String> getAllParameterValues(ParameterManager parameterManager, Map<String, Object> bindings, ObjectPredicate objectPredicate) {
        return parameterManager.getAllParameters(bindings, objectPredicate).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ParameterManager.getMaskedValue(e.getValue())));
    }

    /**
     * The client only ever receives the masked representation of a protected parameter. Saving that
     * representation back unchanged, as the UI does when only the description is edited, must not
     * lose the value. Runs without encryption manager, the value is then stored in clear
     */
    @Test
    public void testSaveOfMaskedProtectedParameterKeepsTheClearValue() {
        // Cloning is enabled to reproduce the behaviour of a real collection, which returns a new
        // instance on every read
        InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>(false);
        ParameterManager m = new ParameterManager(accessor, null, new Configuration(), resolver);

        Parameter parameter = newProtectedParameter();
        m.save(parameter, null, "user");
        ObjectId id = parameter.getId();

        Parameter masked = ParameterManager.maskProtectedValue(accessor.get(id));
        Assert.assertEquals(ParameterManager.PROTECTED_VALUE, masked.getValue().get());
        Assert.assertNull(masked.getEncryptedValue());
        // Masking must not have altered the stored parameter
        Assert.assertEquals(SECRET, accessor.get(id).getValue().get());

        masked.setDescription("edited");
        m.save(masked, accessor.get(id), "user");

        Parameter reloaded = accessor.get(id);
        Assert.assertEquals("edited", reloaded.getDescription());
        Assert.assertEquals(SECRET, reloaded.getValue().get());
    }

    /**
     * Same round trip with an encryption manager: the encrypted value is not returned to the client
     * either, the source parameter is therefore the only authority for it
     */
    @Test
    public void testSaveOfMaskedProtectedParameterKeepsTheEncryptedValue() {
        InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>(false);
        ParameterManager m = new ParameterManager(accessor, ENCRYPTION_MANAGER, new Configuration(), resolver);

        Parameter parameter = newProtectedParameter();
        m.save(parameter, null, "user");
        ObjectId id = parameter.getId();

        Parameter stored = accessor.get(id);
        Assert.assertNull(stored.getValue());
        Assert.assertEquals("###" + SECRET, stored.getEncryptedValue());

        Parameter masked = ParameterManager.maskProtectedValue(accessor.get(id));
        Assert.assertEquals(ParameterManager.PROTECTED_VALUE, masked.getValue().get());
        Assert.assertNull(masked.getEncryptedValue());
        Assert.assertEquals("###" + SECRET, accessor.get(id).getEncryptedValue());

        masked.setDescription("edited");
        m.save(masked, accessor.get(id), "user");

        Parameter reloaded = accessor.get(id);
        Assert.assertEquals("edited", reloaded.getDescription());
        Assert.assertNull(reloaded.getValue());
        Assert.assertEquals("###" + SECRET, reloaded.getEncryptedValue());
    }

    /**
     * Changing the value of a protected parameter must still work
     */
    @Test
    public void testSaveOfProtectedParameterWithNewValue() {
        InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>(false);
        ParameterManager m = new ParameterManager(accessor, ENCRYPTION_MANAGER, new Configuration(), resolver);

        Parameter parameter = newProtectedParameter();
        m.save(parameter, null, "user");
        ObjectId id = parameter.getId();

        Parameter masked = ParameterManager.maskProtectedValue(accessor.get(id));
        masked.setValue(new DynamicValue<>("NewSecret"));
        m.save(masked, accessor.get(id), "user");

        Assert.assertEquals("###NewSecret", accessor.get(id).getEncryptedValue());
    }

    private Parameter newProtectedParameter() {
        Parameter parameter = new Parameter(null, "MySecret", SECRET, "desc");
        parameter.setProtectedValue(true);
        return parameter;
    }

    @Test
    public void testJavascript() throws ScriptException {
        Configuration configuration = new Configuration();
        configuration.putProperty("tec.activator.scriptEngine", "javascript");
        test1Common(configuration);
    }

    @Test
    public void testGroovy() throws ScriptException {
        Configuration configuration = new Configuration();
        configuration.putProperty("tec.activator.scriptEngine", "groovy");
        test1Common(configuration);
    }

    public void test1Common(Configuration configuration) throws ScriptException {
        InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>();
        ParameterManager m = new ParameterManager(accessor, null, configuration, resolver);

        accessor.save(new Parameter(new Expression("user=='pomme'"), "key1", "pommier", "desc"));
        accessor.save(new Parameter(new Expression("user=='pomme'"), "key1", "pommier", "desc"));
        accessor.save(new Parameter(new Expression("user=='abricot'"), "key1", "abricotier", "desc"));
        accessor.save(new Parameter(new Expression("user=='poire'"), "key1", "poirier", "desc"));

        accessor.save(new Parameter(null, "key2", "defaultValue", "desc"));
        accessor.save(new Parameter(null, "key2", "defaultValue2", "desc"));
        accessor.save(new Parameter(new Expression("user=='poire'"), "key2", "defaultValue3", "desc"));

        accessor.save(new Parameter(null, "key3", "value1", "desc"));
        accessor.save(new Parameter(new Expression("user=='poire'"), "key3", "value2", "desc"));
        Parameter p = new Parameter(new Expression("user=='poire'"), "key3", "value3", "desc");
        p.setPriority(10);
        accessor.save(p);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("user", "poire");

        Map<String, String> params = getAllParameterValues(m, bindings, null);
        Assert.assertEquals("poirier", params.get("key1"));
        Assert.assertEquals("defaultValue3", params.get("key2"));
        Assert.assertEquals("value3", params.get("key3"));

        params = getAllParameterValues(m, bindings, t -> false);
        Assert.assertEquals(0, params.size());
    }

    @Category(PerformanceTest.class)
    @Test
    public void testPerf() throws ScriptException {
        Properties properties = new Properties();
        properties.put("host", "central-mongodb.stepcloud-test.ch");
        properties.put("database", "test");
        properties.put("username", "tester");
        properties.put("password", "5dB(rs+4YRJe");
        Collection<Parameter> collection = new MongoDBCollectionFactory(properties).getCollection("perfParameters", Parameter.class);
        AbstractAccessor<Parameter> accessor = new AbstractAccessor<>(collection);
        accessor.getCollectionDriver().remove(Filters.empty());
        ParameterManager m = new ParameterManager(accessor, null, new Configuration(), resolver);

        int nIt = 100;
        for (int i = 1; i <= nIt; i++) {
            accessor.save(new Parameter(new Expression("user=='user" + i + "'"), "key1", "value" + i, "desc"));
        }

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("user", "user" + nIt);

        long t1 = System.currentTimeMillis();
        Map<String, String> params = getAllParameterValues(m, bindings, null);
        logger.info("ms:" + (System.currentTimeMillis() - t1));
        Assert.assertEquals(params.get("key1"), "value" + nIt);

        t1 = System.currentTimeMillis();
        params = getAllParameterValues(m, bindings, null);
        logger.info("ms:" + (System.currentTimeMillis() - t1));
        Assert.assertEquals(params.get("key1"), "value" + nIt);

        Assert.assertTrue((System.currentTimeMillis() - t1) < 3000);
    }

    @Test
    public void testParallel() throws ScriptException, InterruptedException, ExecutionException {
        InMemoryAccessor<Parameter> accessor = new InMemoryAccessor<>();
        ParameterManager m = new ParameterManager(accessor, null, new Configuration(), resolver);

        int nIt = 100;
        for (int i = 1; i <= nIt; i++) {
            accessor.save(new Parameter(new Expression("user=='user" + i + "'"), "key1", "value" + i, "desc"));
        }

        int iterations = 25;

        int nThreads = 4;
        ExecutorService e = Executors.newFixedThreadPool(10);
        List<Future> futures = new ArrayList<>();
        for (int j = 0; j < nThreads; j++) {
            futures.add(e.submit(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iterations; i++) {
                        Map<String, Object> bindings = new HashMap<String, Object>();
                        Random r = new Random();
                        int userId = r.nextInt(nIt) + 1;
                        bindings.put("user", "user" + userId);
                        Map<String, String> params = getAllParameterValues(m, bindings, null);
                        Assert.assertEquals(params.get("key1"), "value" + userId);
                    }
                }
            }));
        }

        e.shutdown();
        e.awaitTermination(1, TimeUnit.MINUTES);

        for (Future f : futures) {
            try {
                f.get();
            } catch (ExecutionException e1) {
                throw e1;
            }
        }
    }
}
