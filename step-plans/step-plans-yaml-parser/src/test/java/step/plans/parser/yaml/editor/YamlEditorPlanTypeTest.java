package step.plans.parser.yaml.editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.PlanCompilationError;
import step.core.plans.PlanCompiler;
import step.core.plans.PlanCompilerException;
import step.plans.parser.yaml.YamlPlanReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class YamlEditorPlanTypeTest {

    private static final Logger log = LoggerFactory.getLogger(YamlEditorPlanTypeTest.class);

    @Test
    public void testNewPlan() throws Exception {
        YamlEditorPlanType yamlEditorPlanType = new YamlEditorPlanType();
        YamlEditorPlan newPlan = yamlEditorPlanType.newPlan("Sequence", "myPlan");
        log.info("Generated source: {}", newPlan.getSource());

        File yamlFile = new File("src/test/resources/step/plans/parser/yaml/editor/test-valid-source.yml");
        try (FileInputStream is = new FileInputStream(yamlFile)) {
            ObjectMapper yamlMapper = YamlPlanReader.createDefaultYamlMapper();
            Assert.assertEquals(yamlMapper.readTree(newPlan.getSource()), yamlMapper.readTree(is));
        }
    }

    @Test
    public void testValidationErrors() throws Exception {
        new CheckValidationErrors() {
            @Override
            protected void doAsserts(List<PlanCompilationError> errors) {
                Assert.assertEquals(1, errors.size());
                Assert.assertEquals("#: 0 subschemas matched instead of one", errors.get(0).getMessage());
                Assert.assertEquals(4, ((YamlEditorPlanTypeCompiler.YamlEditorPlanCompilationError) errors.get(0)).getLine());
            }
        }.check("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-2.yml");

        new CheckValidationErrors() {
            @Override
            protected void doAsserts(List<PlanCompilationError> errors) {
                Assert.assertEquals(1, errors.size());
                Assert.assertEquals("#: required key [name] not found", errors.get(0).getMessage());
                Assert.assertEquals(1, ((YamlEditorPlanTypeCompiler.YamlEditorPlanCompilationError) errors.get(0)).getLine());
            }
        }.check("src/test/resources/step/plans/parser/yaml/invalid/test-invalid-plan-1.yml");
    }

    private abstract static class CheckValidationErrors {
        public void check(String filePath) throws Exception {
            YamlEditorPlanType yamlEditorPlanType = new YamlEditorPlanType();
            PlanCompiler<YamlEditorPlan> compiler = yamlEditorPlanType.getPlanCompiler();

            // read yaml file
            File yamlFile = new File(filePath);

            try (FileInputStream is = new FileInputStream(yamlFile)) {
                byte[] fileContent = is.readAllBytes();
                String fileContentAsString = new String(fileContent);
                YamlEditorPlan newPlan = yamlEditorPlanType.newPlan("Sequence", "myPlan");
                newPlan.setSource(fileContentAsString);

                try {
                    compiler.compile(newPlan);
                    Assert.fail("Validation error expected");
                } catch (PlanCompilerException e) {
                    List<PlanCompilationError> errors = e.getErrors();
                    log.info("Validation errors: {}", errors);

                    doAsserts(errors);
                }
            } catch (IOException e) {
                throw new RuntimeException("IO Exception", e);
            }
        }

        protected abstract void doAsserts(List<PlanCompilationError> errors);
    }

}