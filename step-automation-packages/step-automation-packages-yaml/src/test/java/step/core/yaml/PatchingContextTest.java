package step.core.yaml;

import org.junit.Assert;
import org.junit.Test;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.core.yaml.deserialization.PatchableYamlList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PatchingContextTest {

    private static final String EXPECTED_UNMODIFIED = """
        schemaVersion: 1.0.0
        name: "complete-package"
        fragments:
          - "importPlans.yml"
          - "importKeywords.yml"
        keywords:
          - JMeter:
              name: "JMeter keyword from automation package"
              description: "JMeter keyword 1"
              executeLocally: false
              useCustomTemplate: true
              callTimeout: 1000
              jmeterTestplan: "jmeterProject1/jmeterProject1.xml"
              routing:
                criteriaA: valueA
                criteriaB: valueB
              schema:
                type: object
                properties:
                  firstName:
                    type: string
                  lastName:
                    type: string
                required: [ "firstName", "lastName" ]
          - Composite:
              name: "Composite1"
              plan:
                root:
                  testCase:
                    children:
                      - echo:
                          text: "Just echo"
                      - return:
                          output:
                            - output1: "value"
                            - output2:
                                expression: "'some thing dynamic'"
        plans:
          - name: First Plan
            root:
              sequence:
                continueOnError: false
                children:
                  - if:
                      nodeName: IfBlock
                      condition:
                        expression: "controllerSettings.getSettingByKey('housekeeping_enabled').getValue()=='true'"
                      description: "my description"
                      children:
                        - assert:
                            actual:
                              expression: "'status'"
                            operator: "EQUALS"
                            doNegate: false
                            expected:
                              expression: "'ok'"
                            customErrorMessage: "my custom error"
          - name: Second Plan
            root:
              testCase:
                children:
                  - echo:
                      text: "Just echo"
        schedules:
          - name: "My first task"
            cron: "*/5 * * * *"
            executionParameters:
              environment: "TEST"
            planName: "First Plan"
          - name: "My second task"
            cron: "0 * * * *"
            executionParameters:
              environment: "PROD"
            planName: "Second Plan"
        """;

    private static final String EXPECTED_REMOVED_1 = """
        schemaVersion: 1.0.0
        name: "complete-package"
        fragments:
          - "importPlans.yml"
          - "importKeywords.yml"
        keywords:
          - Composite:
              name: "Composite1"
              plan:
                root:
                  testCase:
                    children:
                      - echo:
                          text: "Just echo"
                      - return:
                          output:
                            - output1: "value"
                            - output2:
                                expression: "'some thing dynamic'"
        plans:
          - name: First Plan
            root:
              sequence:
                continueOnError: false
                children:
                  - if:
                      nodeName: IfBlock
                      condition:
                        expression: "controllerSettings.getSettingByKey('housekeeping_enabled').getValue()=='true'"
                      description: "my description"
                      children:
                        - assert:
                            actual:
                              expression: "'status'"
                            operator: "EQUALS"
                            doNegate: false
                            expected:
                              expression: "'ok'"
                            customErrorMessage: "my custom error"
        schedules:
          - name: "My first task"
            cron: "*/5 * * * *"
            executionParameters:
              environment: "TEST"
            planName: "First Plan"
        """;

    private static final String EXPECTED_REMOVED_2_MODIFIED = """
        schemaVersion: 1.0.0
        name: "complete-package"
        fragments:
          - "importPlans.yml"
          - "importKeywords.yml"
        keywords: []
        plans: []
        schedules:
          - name: "This is now the new schedule name which is rather long, really long in fact"
            active: false
            cron: "*/5 * * * *"
            planName: "First Plan"
            executionParameters: {}
        """;


    private final AutomationPackageDescriptorReader reader;

    public PatchingContextTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        reader = new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, serializationRegistry);
    }

    @Test
    public void testLowLevelDeletionModification() throws Exception {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor2.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "");
            PatchingContext patchingContext = descriptor.getPatchingContext();
            String current = ensureValid(patchingContext.getCurrentYaml());
            Assert.assertEquals(EXPECTED_UNMODIFIED, current);

            // we know that the claimed chunks are in the order [keywords, plans, schedules]
            List<PatchableYamlList> lists = patchingContext.chunks.values().stream()
                .filter(m -> m instanceof PatchableYamlList<?>)
                .map(x -> (PatchableYamlList) x)
                .toList();
            assertEquals(3, lists.size());
            // remove first keyword, second plan, second schedule
            lists.get(0).remove(0);
            lists.get(1).remove(1);
            lists.get(2).remove(1);

            current = ensureValid(patchingContext.getCurrentYaml());
            Assert.assertEquals(EXPECTED_REMOVED_1, current);

            // empty first two lists, modify schedule a little
            AutomationPackageSchedule schedule1 = (AutomationPackageSchedule) lists.get(2).getFirst();
            schedule1.setName("This is now the new schedule name which is rather long, really long in fact");
            schedule1.setActive(false);
            schedule1.getExecutionParameters().clear();
            // we have to tell the entity explicitly that it was modified
            schedule1.setModified();
            lists.subList(0, 2).forEach(ArrayList::removeFirst);

            current = ensureValid(patchingContext.getCurrentYaml());
            Assert.assertEquals(EXPECTED_REMOVED_2_MODIFIED, current);

            lists.get(2).removeFirst();
            current = ensureValid(patchingContext.getCurrentYaml());

            Assert.assertEquals("""
                schemaVersion: 1.0.0
                name: "complete-package"
                fragments:
                  - "importPlans.yml"
                  - "importKeywords.yml"
                keywords: []
                plans: []
                schedules: []
                """, current);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String ensureValid(String yaml) throws Exception {
        InputStream in = new ByteArrayInputStream(yaml.getBytes());
        // we're simply reading it and expecting it not to fail.
        reader.readAutomationPackageDescriptor(in, "");
        return yaml;
    }
}
