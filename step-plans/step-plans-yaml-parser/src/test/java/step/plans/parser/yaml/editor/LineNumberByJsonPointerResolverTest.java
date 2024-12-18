package step.plans.parser.yaml.editor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LineNumberByJsonPointerResolverTest {

    private static final Logger log = LoggerFactory.getLogger(LineNumberByJsonPointerResolverTest.class);

    private final LineNumberByJsonPointerResolver resolver = new LineNumberByJsonPointerResolver();

    @Before
    public void beforeEach(){
    }

    @Test
    public void testJsonPointerToJsonPath() {
        // it should return "$." for empty json pointer
        Assert.assertEquals("$", resolver.jsonPointerToJsonPath(""));

        try {
            // it should throw an error for an invalid json path
            resolver.jsonPointerToJsonPath("prop/childProp");
            Assert.fail("Exception is not thrown");
        } catch (Exception ex) {
            // ok
        }

        // it should convert a simple json pointer to json path
        Assert.assertEquals("$.prop.childProp", resolver.jsonPointerToJsonPath("/prop/childProp"));

        // it should convert a simple json pointer to json path
        Assert.assertEquals("$.prop.1.childProp", resolver.jsonPointerToJsonPath("/prop/1/childProp"));

        // it should convert a json pointer with a property access with special characters
        Assert.assertEquals("$.k\"l.1.childProp", resolver.jsonPointerToJsonPath("/k\"l/1/childProp"));

        // it should convert a json pointer with a property access with dot on it
        Assert.assertEquals("$.k\"l..1.childProp", resolver.jsonPointerToJsonPath("/k\"l./1/childProp"));

        // it should convert a json pointer with a property access with dot on it
        Assert.assertEquals("$.root", resolver.jsonPointerToJsonPath("#/root"));
    }

    @Test
    public void testLineNrResolve() {
        // read yaml file
        File yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml");

        try (FileInputStream is = new FileInputStream(yamlFile)) {
            List<LineNumberByJsonPointerResolver.JsonPointerSourceLine> found = resolver.findLineNumbers(List.of("#/root", "#"), new String(is.readAllBytes()));
            Assert.assertEquals(
                    List.of(
                            new LineNumberByJsonPointerResolver.JsonPointerSourceLine("#/root", 4),
                            new LineNumberByJsonPointerResolver.JsonPointerSourceLine("#", 1)
                    ),
                    found
            );
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }

    }

    @Test
    public void testLeaks() {
        // read another yaml file - the location mapping should NOT be accumulated
        File yamlFile = new File("src/test/resources/step/plans/parser/yaml/basic/test.plan.yml");

        try (FileInputStream is = new FileInputStream(yamlFile)) {
            LineNumberByJsonPointerResolver.LineNumberResolveInternalResult res = resolver.findLineNumbersInternal(List.of("#/root", "#"), new String(is.readAllBytes()), true);

            // 47 nodes in yml
            Assert.assertEquals(47, res.getAllocationMapping().size());
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }

        yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml");
        try (FileInputStream is = new FileInputStream(yamlFile)) {
            LineNumberByJsonPointerResolver.LineNumberResolveInternalResult res = resolver.findLineNumbersInternal(List.of("#/root"), new String(is.readAllBytes()), true);

            // 11 nodes in yml
            Assert.assertEquals(11, res.getAllocationMapping().size());
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }

    @Test
    public void testIsolation() throws ExecutionException, InterruptedException {
        final int iterations = 10000;
        AtomicBoolean firstThreadOk = new AtomicBoolean(true);
        AtomicBoolean secondThreadOk = new AtomicBoolean(true);
        AtomicInteger firstThreadCount = new AtomicInteger(0);
        AtomicInteger secondThreadCount = new AtomicInteger(0);

        // run two threads concurrently
        Future<?> future1 = Executors.newSingleThreadExecutor().submit(() -> {
            // read another yaml file - the location mapping should NOT be accumulated
            File yamlFile = new File("src/test/resources/step/plans/parser/yaml/basic/test.plan.yml");
            String content;
            try (FileInputStream is = new FileInputStream(yamlFile)) {
                content = new String(is.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException("IO Exception", e);
            }

            // run in cycle until both threads perform enough iterations
            log.info("First thread has been started");
            while (secondThreadOk.get() && (firstThreadCount.get() < iterations || secondThreadCount.get() < iterations)) {
                firstThreadCount.incrementAndGet();
                LineNumberByJsonPointerResolver.LineNumberResolveInternalResult res = resolver.findLineNumbersInternal(List.of("#/root", "#"), content, true);

                // 47 nodes in yml
                int expectedNodes = 47;
                if (!Objects.equals(expectedNodes, res.getAllocationMapping().size())) {
                    log.error("First thread: total count ({}) doesn't match (expected {}). Failed on iteration {}", res.getAllocationMapping().size(), expectedNodes, firstThreadCount.get());
                    firstThreadOk.set(false);
                    return;
                }
            }
            log.info("First thread OK ({} iterations done)", firstThreadCount.get());
            firstThreadOk.set(true);
        });

        Future<?> future2 = Executors.newSingleThreadExecutor().submit(() -> {
            // read another yaml file - the location mapping should NOT be accumulated
            File yamlFile = new File("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml");
            String content;
            try (FileInputStream is = new FileInputStream(yamlFile)) {
                content = new String(is.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException("IO Exception", e);
            }

            // run in cycle until both threads perform enough iterations
            log.info("Second thread has been started");
            while (firstThreadOk.get() && (firstThreadCount.get() < iterations || secondThreadCount.get() < iterations)) {
                secondThreadCount.incrementAndGet();

                LineNumberByJsonPointerResolver.LineNumberResolveInternalResult res = resolver.findLineNumbersInternal(List.of("#/root", "#"), content, true);

                // 11 nodes in yml
                int expectedNodes = 11;
                if (!Objects.equals(expectedNodes, res.getAllocationMapping().size())) {
                    log.error("Second thread: total count ({}) doesn't match (expected {}). Failed on iteration {}", res.getAllocationMapping().size(), expectedNodes, secondThreadCount.get());
                    secondThreadOk.set(false);
                    return;
                }
            }
            log.info("Second thread OK ({} iterations done)", secondThreadCount.get());
            secondThreadOk.set(true);
        });

        future1.get();
        future2.get();
        Assert.assertTrue("First thread is not ok", firstThreadOk.get());
        Assert.assertTrue("Second thread is not ok", secondThreadOk.get());
    }

}