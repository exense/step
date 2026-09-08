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
import step.automation.packages.AutomationPackageReadingException;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class AutomationPackagePlanFieldOrderingTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackagePlanFieldOrderingTest() {
        super(new File("src/test/resources/testdata/ap-field-ordering"));
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    @Test
    public void testPlanFieldOrdering() throws IOException {

        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "FieldOrdering"), null, null, null, 100).findFirst();

        Assert.assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();
        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("FieldOrdering.yml"), destinationDirectory.toPath().resolve("plans").resolve("FieldOrdering.yml"));
    }
}
