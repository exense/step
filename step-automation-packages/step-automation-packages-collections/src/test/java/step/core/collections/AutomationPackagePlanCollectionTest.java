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
package step.core.collections;

import org.junit.Before;
import org.junit.Test;
import step.artefacts.Echo;
import step.artefacts.Sequence;
import step.automation.packages.AutomationPackageReadingException;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.plans.parser.yaml.YamlPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutomationPackagePlanCollectionTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackagePlanCollectionTest() {
        super();
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    @Test
    public void testReadAllPlans() {
        long count = planCollection.count(Filters.empty(), 100);
        List<Plan> plans = planCollection.find(Filters.empty(), null, null, null, 100).collect(Collectors.toList());

        assertEquals(2, count);
        Set<String> names = plans.stream().map(p -> p.getAttributes().get("name")).collect(Collectors.toUnmodifiableSet());

        assertEquals(2, names.size());

        assertTrue(names.contains("Test Plan"));
        assertTrue(names.contains("Test Plan with Composite"));
    }

    @Test
    public void testPlanModify() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        Echo firstEcho = (Echo) plan.getRoot().getChildren().get(0);
        DynamicValue<Object> text = firstEcho.getText();
        text.setDynamic(true);
        text.setExpression("new Date().toString();");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterModification.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }


    @Test
    public void testPlanModifyWithConcurrentEdit() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        Echo firstEcho = (Echo) plan.getRoot().getChildren().get(0);
        DynamicValue<Object> text = firstEcho.getText();
        text.setDynamic(true);
        text.setExpression("new Date().toString();");

        Files.copy(sourceDirectory.toPath().resolve("plans").resolve("plan1.yml"),
            destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"),
            StandardCopyOption.REPLACE_EXISTING);

        assertThrows(AutomationPackageConcurrentEditException.class, () -> planCollection.save(plan));

    }


    @Test
    public void testPlanRenameExisting() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        plan.getAttributes().put("name", "New Plan Name");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterRename.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }


    @Test
    public void testPlanRemoveExisting() throws IOException {
        planCollection.remove(Filters.equals("attributes.name", "Test Plan"));

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterRemove.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }

    @Test
    public void testAddPlanToExistingFragmentWithExistingPlans() throws IOException {

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        Plan plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "New Name");
        plan.setAttributes(attributes);


        setPropertiesWriteToFragment(YamlPlan.PLANS_ENTITY_NAME, "plans/plan1.yml");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterAdd.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }

    @Test
    public void testPlanModifyAndAdd() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        Echo firstEcho = (Echo) plan.getRoot().getChildren().get(0);
        DynamicValue<Object> text = firstEcho.getText();
        text.setDynamic(true);
        text.setExpression("new Date().toString();");

        planCollection.save(plan);

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "New Name");
        plan.setAttributes(attributes);

        setPropertiesWriteToFragment(YamlPlan.PLANS_ENTITY_NAME, "plans/plan1.yml");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterModifyAndAdd.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }

    @Test
    public void testPlanModifyAndAddAndRemove() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        Echo firstEcho = (Echo) plan.getRoot().getChildren().get(0);
        DynamicValue<Object> text = firstEcho.getText();
        text.setDynamic(true);
        text.setExpression("new Date().toString();");

        planCollection.save(plan);

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "New Name");
        plan.setAttributes(attributes);


        setPropertiesWriteToFragment(YamlPlan.PLANS_ENTITY_NAME, "plans/plan1.yml");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterModifyAndAdd.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));

        planCollection.remove(Filters.equals("attributes.name", "New Name"));


        assertFilesEqual(expectedFilesPath.resolve("plan1AfterModification.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }

    @Test
    public void testAddPlanToDescriptorWithPresentButEmptyPlanArray() throws IOException {

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        Plan plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "New Name");
        plan.setAttributes(attributes);

        setPropertiesWriteToFragment(YamlPlan.PLANS_ENTITY_NAME, "automation-package.yml");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("descriptorAfterAdd.yml"), destinationDirectory.toPath().resolve("automation-package.yml"));
    }


    @Test
    public void testAddPlanToNewFragment() throws IOException {

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        Plan plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "Hello World Plan");
        plan.setAttributes(attributes);

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("Hello World Plan.yml"), destinationDirectory.toPath().resolve("plans").resolve("Hello World Plan.yml"));
    }
}
