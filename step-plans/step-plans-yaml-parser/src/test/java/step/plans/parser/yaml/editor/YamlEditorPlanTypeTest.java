package step.plans.parser.yaml.editor;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.PlanCompilationError;
import step.core.plans.PlanCompiler;
import step.core.plans.PlanCompilerException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class YamlEditorPlanTypeTest {

    private static final Logger log = LoggerFactory.getLogger(YamlEditorPlanTypeTest.class);

    @Test
    public void testValidationErrors() throws Exception {
        YamlEditorPlanType yamlEditorPlanType = new YamlEditorPlanType();
        PlanCompiler<YamlEditorPlan> compiler = yamlEditorPlanType.getPlanCompiler();

        String invalidPlanFile = "src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml";

        // read yaml file
        File yamlFile = new File(invalidPlanFile);

        try (FileInputStream is = new FileInputStream(yamlFile); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] fileContent = is.readAllBytes();
            String fileContentAsString = new String(fileContent);
            YamlEditorPlan newPlan = yamlEditorPlanType.newPlan("Sequence");
            newPlan.setSource(fileContentAsString);

            try {
                YamlEditorPlan result = compiler.compile(newPlan);
                Assert.fail("Validation error expected");
            } catch (PlanCompilerException e){
                List<PlanCompilationError> errors = e.getErrors();
                log.info("Validation errors: {}", errors);

                Assert.assertEquals(1, errors.size());

                // TODO: fix check (resolve line correctly)
                Assert.assertEquals("#: 0 subschemas matched instead of one", errors.get(0).getMessage());
                Assert.assertEquals(1, ((YamlEditorPlanTypeCompiler.YamlEditorPlanCompilationError) errors.get(0)).getLine());

            }
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }

}