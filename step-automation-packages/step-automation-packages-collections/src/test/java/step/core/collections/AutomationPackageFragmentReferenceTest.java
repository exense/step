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
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.plans.parser.yaml.YamlPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AutomationPackageFragmentReferenceTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackageFragmentReferenceTest() {
        super();
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }


    @Test
    public void testAddPlanToNewFragmentAndRenameAndRemove() throws IOException {

        Sequence sequence = new Sequence();
        Echo echo = new Echo();
        echo.setText(new DynamicValue<>("Hello World"));
        sequence.addChild(echo);

        Plan plan = new Plan(sequence);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(AbstractArtefact.NAME, "Hello World Plan");
        plan.setAttributes(attributes);

        setPropertiesWriteMode(YamlPlan.PLANS_ENTITY_NAME, "newPlansPath", AutomationPackageYamlFragmentManager.NewObjectFragmentMode.PER_OBJECT);

        planCollection.save(plan);

        assertFilesEqual(
            expectedFilesPath
                .resolve("Hello World Plan.yml"),
            destinationDirectory.toPath()
                .resolve("newPlansPath")
                .resolve("Hello World Plan.yml")
        );
        assertFilesEqual(
            expectedFilesPath
                .resolve("descriptorAfterNewFragmentReference.yml"),
            destinationDirectory.toPath()
                .resolve("automation-package.yml")
        );

        attributes.put(AbstractArtefact.NAME, "This Plan was renamed");

        planCollection.save(plan);

        assertFilesEqual(
            expectedFilesPath
                .resolve("This Plan was renamed.yml"),
            destinationDirectory.toPath()
                .resolve("newPlansPath")
                .resolve("This Plan was renamed.yml")
        );

        assertFilesEqual(
            expectedFilesPath
                .resolve("descriptorAfterNewFragmentReference.yml"),
            destinationDirectory.toPath()
                .resolve("automation-package.yml"));

        planCollection.remove(Filters.equals("attributes.name", "This Plan was renamed"));

        assertFalse(Files.exists(
            destinationDirectory.toPath()
                .resolve("newPlansPath")
                .resolve("This Plan was renamed.yml")
        ));

        assertFilesEqual(
            expectedFilesPath
                .resolve("descriptorAfterNewFragmentReference.yml"),
            destinationDirectory.toPath()
                .resolve("automation-package.yml"));
    }
}
