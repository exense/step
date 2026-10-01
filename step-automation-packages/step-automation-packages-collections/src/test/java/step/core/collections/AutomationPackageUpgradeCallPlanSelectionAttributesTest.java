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

/**
 * The plans of a package declaring an older schema version are migrated when read, and written back with the current
 * syntax once saved
 */
public class AutomationPackageUpgradeCallPlanSelectionAttributesTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackageUpgradeCallPlanSelectionAttributesTest() {
        super(new File("src/test/resources/testdata/ap-upgrade-call-plan-selection-attributes"));
    }

    @Override
    protected boolean upgradeOnLoad() {
        return true;
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    /**
     * The call plans select the called plan with the selectionAttributes of the schema 1.2.0. Once the package is
     * upgraded, they are written with the plan field exactly as the same plan authored against the current schema
     */
    @Test
    public void testCallPlanSelectionAttributesAreMigrated() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "CallEntities"), null, null, null, 100).findFirst();

        Assert.assertTrue(optionalPlan.isPresent());

        planCollection.save(optionalPlan.get());

        assertFilesEqual(expectedFilesPath.resolve("CallEntities.yml"), destinationDirectory.toPath().resolve("plans").resolve("CallEntities.yml"));
    }
}
