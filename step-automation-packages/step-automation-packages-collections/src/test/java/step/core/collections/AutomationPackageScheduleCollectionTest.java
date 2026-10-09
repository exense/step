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
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityConstants;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutomationPackageScheduleCollectionTest extends AutomationPackageCollectionTestBase {

    private Collection<ExecutiontTaskParameters> scheduleCollection;
    private Collection<Plan> planCollection;

    @Override
    protected void registerAdditionalEntities(AutomationPackageSerializationRegistry serializationRegistry) {
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        scheduleCollection = collectionFactory.getCollection(EntityConstants.tasks, ExecutiontTaskParameters.class);
        planCollection = collectionFactory.getCollection(EntityConstants.plans, Plan.class);
    }

    @Test
    public void testScheduleRead() {
        ExecutiontTaskParameters schedule = findSchedule("firstSchedule");
        assertTrue(schedule.isActive());
        assertEquals("0 15 10 ? * *", schedule.getCronExpression());
        assertEquals(2, schedule.getCronExclusions().size());
        assertEquals("0 0 9 25 * ?", schedule.getCronExclusions().get(0).getCronExpression());

        // The plan is defined in another fragment than the schedule
        Plan plan = findPlan("Test Plan");
        ExecutionParameters executionParameters = schedule.getExecutionsParameters();
        assertEquals("Test Plan", executionParameters.getDescription());
        assertEquals(RepositoryObjectReference.LOCAL_REPOSITORY_ID, executionParameters.getRepositoryObject().getRepositoryID());
        assertEquals(plan.getId().toString(), executionParameters.getRepositoryObject().getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID));
    }

    @Test
    public void testScheduleModify() throws IOException {
        ExecutiontTaskParameters schedule = findSchedule("firstSchedule");
        schedule.setCronExpression("0 0 12 ? * *");
        schedule.setActive(false);
        schedule.setCronExclusions(null);
        scheduleCollection.save(schedule);

        String yaml = readSchedulesFragment();
        assertTrue(yaml, yaml.contains("0 0 12 ? * *"));
        assertTrue(yaml, yaml.contains("Test Plan"));
        assertTrue(yaml, yaml.contains("active: false"));
        assertFalse(yaml, yaml.contains("0 15 10 ? * *"));
        assertFalse(yaml, yaml.contains("cronExclusions"));
    }

    @Test
    public void testScheduleAddAndRemove() throws IOException {
        Plan plan = findPlan("Test Plan with Composite");
        Map<String, String> repositoryParameters = new HashMap<>();
        repositoryParameters.put(RepositoryObjectReference.PLAN_ID, plan.getId().toString());
        RepositoryObjectReference repositoryObject = new RepositoryObjectReference(RepositoryObjectReference.LOCAL_REPOSITORY_ID, repositoryParameters);
        ExecutiontTaskParameters schedule = new ExecutiontTaskParameters(new ExecutionParameters(repositoryObject, Map.of("env", "TEST")), "0 0 6 ? * *");
        schedule.addAttribute(AbstractOrganizableObject.NAME, "addedSchedule");
        scheduleCollection.save(schedule);

        // A new schedule is written to a file of its own, named after it and referenced with a wildcard
        Path scheduleFile = destinationDirectory.toPath().resolve("schedules").resolve("addedSchedule.yml");
        String yaml = Files.readString(scheduleFile);
        assertTrue(yaml, yaml.contains("addedSchedule"));
        assertTrue(yaml, yaml.contains("0 0 6 ? * *"));
        // The plan is referred to by its name, resolved from its id
        assertTrue(yaml, yaml.contains("Test Plan with Composite"));
        assertTrue(yaml, yaml.contains("env"));
        String descriptor = Files.readString(destinationDirectory.toPath().resolve("automation-package.yml"));
        assertTrue(descriptor, descriptor.contains("schedules/*.yml"));
        assertFalse(readSchedulesFragment().contains("addedSchedule"));

        scheduleCollection.remove(Filters.equals("attributes.name", "addedSchedule"));
        assertEquals(0, scheduleCollection.count(Filters.equals("attributes.name", "addedSchedule"), null));
        if (Files.exists(scheduleFile)) {
            yaml = Files.readString(scheduleFile);
            assertFalse(yaml, yaml.contains("addedSchedule"));
        }
        assertTrue(readSchedulesFragment().contains("firstSchedule"));
    }

    private ExecutiontTaskParameters findSchedule(String name) {
        return scheduleCollection.find(Filters.equals("attributes.name", name), null, null, null, 0).findFirst().orElseThrow();
    }

    private Plan findPlan(String name) {
        return planCollection.find(Filters.equals("attributes.name", name), null, null, null, 0).findFirst().orElseThrow();
    }

    private String readSchedulesFragment() throws IOException {
        return Files.readString(destinationDirectory.toPath().resolve("schedules.yml"));
    }
}
