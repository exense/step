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

import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.Sequence;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageReadOnlyEntityException;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityConstants;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutomationPackagePlainTextPlanCollectionTest extends AutomationPackageCollectionTestBase {

    private static final String SINGLE_PLAIN_TEXT_PLAN = "Single plain text plan";

    private Collection<Plan> planCollection;
    private Collection<ExecutiontTaskParameters> scheduleCollection;

    public AutomationPackagePlainTextPlanCollectionTest() {
        super(new File("src/test/resources/testdata/ap-with-plain-text-plans"));
    }

    @Override
    protected void registerAdditionalEntities(AutomationPackageSerializationRegistry serializationRegistry) {
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(EntityConstants.plans, Plan.class);
        scheduleCollection = collectionFactory.getCollection(EntityConstants.tasks, ExecutiontTaskParameters.class);
    }

    @Test
    public void testPlainTextPlansAreReadAsReadOnlyPlans() {
        List<Plan> plans = planCollection.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        Set<String> names = plans.stream().map(AutomationPackagePlainTextPlanCollectionTest::name).collect(Collectors.toSet());
        assertEquals(Set.of("Yaml Plan", SINGLE_PLAIN_TEXT_PLAN, "firstPlainText.plan", "secondPlainText.plan"), names);

        for (Plan plan : plans) {
            boolean plainText = !name(plan).equals("Yaml Plan");
            assertEquals(name(plan), plainText, AutomationPackageYamlFragmentManager.isReadOnly(plan));
            if (plainText) {
                assertEquals(AutomationPackageYamlFragmentManager.PLAIN_TEXT_PLAN_READ_ONLY_REASON, plan.getCustomField(AutomationPackageYamlFragmentManager.CUSTOM_FIELD_READ_ONLY_REASON));
            } else {
                assertNull(plan.getCustomField(AutomationPackageYamlFragmentManager.CUSTOM_FIELD_READ_ONLY));
                assertNull(plan.getCustomField(AutomationPackageYamlFragmentManager.CUSTOM_FIELD_READ_ONLY_REASON));
            }
        }

        Plan singlePlan = findPlan(SINGLE_PLAIN_TEXT_PLAN);
        assertTrue(singlePlan.getRoot() instanceof Sequence);
        assertEquals(List.of("Plain text"), singlePlan.getCategories());
    }

    @Test
    public void testPlainTextPlanCannotBeSaved() throws IOException {
        Map<String, String> filesBefore = readPackageFiles();
        Plan plan = findPlan(SINGLE_PLAIN_TEXT_PLAN);
        plan.getAttributes().put(AbstractOrganizableObject.NAME, "Renamed");

        AutomationPackageReadOnlyEntityException exception = assertThrows(AutomationPackageReadOnlyEntityException.class, () -> planCollection.save(plan));
        assertTrue(exception.getMessage(), exception.getMessage().contains(AutomationPackageYamlFragmentManager.PLAIN_TEXT_PLAN_READ_ONLY_REASON));

        // neither the collection nor the files of the package are changed
        assertEquals(1, planCollection.count(Filters.equals("attributes.name", SINGLE_PLAIN_TEXT_PLAN), null));
        assertEquals(0, planCollection.count(Filters.equals("attributes.name", "Renamed"), null));
        assertEquals(filesBefore, readPackageFiles());
    }

    @Test
    public void testPlainTextPlanCannotBeRemoved() throws IOException {
        Map<String, String> filesBefore = readPackageFiles();

        // The YAML plan matching the filter is not removed either
        assertThrows(AutomationPackageReadOnlyEntityException.class, () -> planCollection.remove(Filters.empty()));

        assertEquals(4, planCollection.count(Filters.empty(), null));
        assertEquals(filesBefore, readPackageFiles());
    }

    @Test
    public void testCopyOfPlainTextPlanIsSavedAsYamlPlan() throws AutomationPackageReadingException {
        // As cloned by the plan services, which give the clone a new id and no custom field
        Plan copy = findPlan(SINGLE_PLAIN_TEXT_PLAN);
        copy.setId(new ObjectId());
        copy.setCustomFields(null);
        copy.getAttributes().put(AbstractOrganizableObject.NAME, "Copy");

        planCollection.save(copy);

        assertFalse(AutomationPackageYamlFragmentManager.isReadOnly(findPlan("Copy")));
        assertTrue(new File(destinationDirectory, "plans/Copy.yml").isFile());

        // Read again, the copy is a YAML plan and the plain text plan is still there
        AutomationPackageYamlFragmentManager reloaded = reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
        Iterable<Plan> reloadedPlans = reloaded.getBusinessObjects(Plan.class);
        for (Plan plan : reloadedPlans) {
            assertEquals(name(plan), !Set.of("Yaml Plan", "Copy").contains(name(plan)), AutomationPackageYamlFragmentManager.isReadOnly(plan));
        }
    }

    @Test
    public void testScheduleRefersToPlainTextPlan() {
        ExecutiontTaskParameters schedule = scheduleCollection.find(Filters.equals("attributes.name", "plainTextSchedule"), null, null, null, 0).findFirst().orElseThrow();
        Plan plan = findPlan(SINGLE_PLAIN_TEXT_PLAN);
        assertEquals(plan.getId().toString(), schedule.getExecutionsParameters().getRepositoryObject().getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID));
    }

    private Plan findPlan(String name) {
        return planCollection.find(Filters.equals("attributes.name", name), null, null, null, 0).findFirst().orElseThrow();
    }

    private static String name(Plan plan) {
        return plan.getAttribute(AbstractOrganizableObject.NAME);
    }

    private Map<String, String> readPackageFiles() throws IOException {
        Map<String, String> files = new TreeMap<>();
        for (File file : FileUtils.listFiles(destinationDirectory, null, true)) {
            files.put(destinationDirectory.toPath().relativize(file.toPath()).toString(), FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        }
        return files;
    }
}
