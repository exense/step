package step.plans.parser.yaml.editor;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class LineNumberByJsonPointerResolverTest {

    private final LineNumberByJsonPointerResolver resolver = new LineNumberByJsonPointerResolver();

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
            String fullJson = new String(is.readAllBytes());
            Assert.assertEquals(
                    List.of(
                            new LineNumberByJsonPointerResolver.JsonPointerSourceLine("#/root", 4),
                            new LineNumberByJsonPointerResolver.JsonPointerSourceLine("#", 1)
                    ),
                    resolver.findLineNumbers(List.of("#/root", "#"), fullJson)
            );
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }

    }

}