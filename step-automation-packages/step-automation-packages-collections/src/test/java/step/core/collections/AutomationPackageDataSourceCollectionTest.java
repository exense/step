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
import step.artefacts.DataSetArtefact;
import step.automation.packages.AutomationPackageReadingException;
import step.core.plans.Plan;
import step.datapool.excel.ExcelDataPool;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The data source of a plan is a reference to a file of the automation package, and the only one the
 * keyword plugins do not map themselves: it is applied by
 * {@code AutomationPackagePlansAttributesApplier} on the way in - at deploy time and, with the local
 * mapper, in the editor - and put back in the form the descriptor holds on the way out.
 */
public class AutomationPackageDataSourceCollectionTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackageDataSourceCollectionTest() {
        super(new File("src/test/resources/testdata/ap-with-datasource"));
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    @Test
    public void testDataSourceIsReadAsALocalApResource() {
        assertEquals("apResource:local:data/pool.xlsx", dataSourceFileOfTheOnlyPlan());
    }

    @Test
    public void testDataSourceIsWrittenBackAsARelativePath() throws IOException {
        Plan plan = onlyPlan();

        planCollection.save(plan);

        String descriptor = Files.readString(destinationDirectory.toPath().resolve("automation-package.yml"));
        assertTrue(descriptor, descriptor.contains("file: \"data/pool.xlsx\""));
        assertFalse(descriptor, descriptor.contains("apResource:"));
        // and the live entity keeps the reference form for the rest of the editing session
        assertEquals("apResource:local:data/pool.xlsx", dataSourceFileOfTheOnlyPlan());
    }

    private String dataSourceFileOfTheOnlyPlan() {
        DataSetArtefact dataSet = (DataSetArtefact) onlyPlan().getRoot().getChildren().get(0);
        return ((ExcelDataPool) dataSet.getDataSource()).getFile().get();
    }

    private Plan onlyPlan() {
        return planCollection.find(Filters.empty(), null, null, null, 100).findFirst().orElseThrow();
    }
}
