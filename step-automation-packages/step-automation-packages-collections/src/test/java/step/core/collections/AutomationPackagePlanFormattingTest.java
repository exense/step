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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.Script;
import step.automation.packages.AutomationPackageReadingException;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class AutomationPackagePlanFormattingTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackagePlanFormattingTest() {
        super(new File("src/test/resources/testdata/ap-formatting"));
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    @Test
    public void testPlanFieldOrdering() throws IOException {

        planCollection.save(loadPlan("FieldOrdering"));
        planCollection.save(loadPlan("Some plan"));

        assertFilesEqual(expectedFilesPath.resolve("FieldOrdering.yml"), destinationDirectory.toPath().resolve("plans").resolve("FieldOrdering.yml"));
    }

    @Test
    public void testCallEntities() throws IOException {
        planCollection.save(loadPlan("CallEntities"));
        assertFilesEqual(expectedFilesPath.resolve("CallEntities.yml"), destinationDirectory.toPath().resolve("plans").resolve("CallEntities.yml"));
    }

    @Test
    public void testMultiLineYamlStringsNoModification() throws IOException {
        planCollection.save(loadPlan("MultiLineScalars - Single Line Field with empty line"));
        planCollection.save(loadPlan("MultiLineScalars - Before single line field"));
        planCollection.save(loadPlan("MultiLineScalars - End of Object with empty line"));
        planCollection.save(loadPlan("MultiLineScalars - End of Object"));
        planCollection.save(loadPlan("MultiLineScalars - End of File"));

        assertFilesEqual(expectedFilesPath.resolve("MultiLineScalarsNoModification.yml"), destinationDirectory.toPath().resolve("plans").resolve("MultiLineScalars.yml"));
    }

    @Test
    public void testMultiLineYamlStringsAfterModification() throws IOException {
        Plan plan = loadPlan("MultiLineScalars - Before single line field");

        plan.getRoot().getChildren().get(0).setDescription("""
            This description was expanded upon and now stretches...

            ...over multiple lines
            """);

        planCollection.save(plan);

        plan = loadPlan("MultiLineScalars - End of Object with empty line");

        Script script = (Script) plan.getRoot().getChildren().get(0);
        script.setDescription("The new description fits one line");
        script.setScript("nothing");

        planCollection.save(plan);

        plan = loadPlan("MultiLineScalars - End of Object");

        planCollection.save(plan);

        plan = loadPlan("MultiLineScalars - End of File");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("MultiLineScalarsAfterModification.yml"), destinationDirectory.toPath().resolve("plans").resolve("MultiLineScalars.yml"));
    }

    private Plan loadPlan(String name) {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", name), null, null, null, 100).findFirst();
        Assert.assertTrue(optionalPlan.isPresent());
        return optionalPlan.get();
    }
}
