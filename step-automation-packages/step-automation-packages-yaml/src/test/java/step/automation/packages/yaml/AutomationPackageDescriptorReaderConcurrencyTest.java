/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.automation.packages.yaml;

import ch.exense.commons.test.categories.PerformanceTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.core.yaml.PatchingContext;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * The controller reads every automation package with one shared descriptor reader. Packages uploaded in parallel
 * are read concurrently by it, including the very first reads, while its object mapper has nothing cached yet.
 */
public class AutomationPackageDescriptorReaderConcurrencyTest {

    private static final int ROUNDS = 50;
    private static final int THREADS = 8;

    @Test
    @Category(PerformanceTest.class)
    public void concurrentFirstReads() throws Exception {
        byte[] descriptor = Files.readAllBytes(Path.of("src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor.yml"));
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Throwable> failures = new ArrayList<>();
        try {
            for (int round = 0; round < ROUNDS; round++) {
                // a new reader per round, so that each round starts with an empty deserializer cache
                AutomationPackageDescriptorReader reader = createReader();
                CyclicBarrier start = new CyclicBarrier(THREADS);
                List<Future<AutomationPackageDescriptorYaml>> results = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    results.add(executor.submit(() -> {
                        start.await(10, TimeUnit.SECONDS);
                        try (InputStream is = new ByteArrayInputStream(descriptor)) {
                            return reader.readAutomationPackageDescriptor(is, "");
                        }
                    }));
                }
                for (Future<AutomationPackageDescriptorYaml> result : results) {
                    try {
                        assertOwnPatchingContext(result.get(30, TimeUnit.SECONDS));
                    } catch (Throwable e) {
                        failures.add(e.getCause() != null ? e.getCause() : e);
                    }
                }
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        failures.forEach(Throwable::printStackTrace);
        assertEquals("Failed concurrent reads (first: " + (failures.isEmpty() ? "-" : failures.get(0)) + ")",
            0, failures.size());
    }

    /**
     * The models of a descriptor are patched back into the YAML of the read that created them, so each of them must
     * hold the patching context of that read, not the one of a read running at the same time.
     */
    private static void assertOwnPatchingContext(AutomationPackageDescriptorYaml descriptor) {
        assertNotNull(descriptor);
        PatchingContext context = descriptor.getPatchingContext();
        assertNotNull(context);
        assertSame("version", context, descriptor.getVersion().getPatchingContext());
        assertFalse(descriptor.getKeywords().isEmpty());
        descriptor.getKeywords().forEach(k -> assertSame("keyword", context, k.getPatchingContext()));
        assertFalse(descriptor.getPlans().isEmpty());
        descriptor.getPlans().forEach(p -> assertSame("plan", context, p.getPatchingContext()));
    }

    private static AutomationPackageDescriptorReader createReader() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        serializationRegistry.register(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP, AutomationPackageParameter.class);
        return new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, serializationRegistry);
    }
}
