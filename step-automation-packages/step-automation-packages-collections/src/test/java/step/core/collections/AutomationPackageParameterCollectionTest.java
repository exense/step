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
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserialization.AutomationPackagePerObjectSaveUnsupportedException;
import step.parameter.Parameter;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutomationPackageParameterCollectionTest extends AutomationPackageCollectionTestBase {

    private Collection<Parameter> parameterCollection;

    public AutomationPackageParameterCollectionTest() {
        super();
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        parameterCollection = collectionFactory.getCollection(Parameter.ENTITY_NAME, Parameter.class);
    }

    @Test
    public void testParameterModify() throws IOException {
        Optional<Parameter> optionalParameter = parameterCollection.find(Filters.equals("key", "mySimpleKey"), null, null, null, 100).findFirst();

        assertTrue(optionalParameter.isPresent());

        Parameter parameter = optionalParameter.get();

        parameter.getValue().setValue("myModifiedValue");

        setPropertiesWriteToFragment(Parameter.ENTITY_NAME, "parameters.yml");

        parameterCollection.save(parameter);

        assertFilesEqual(expectedFilesPath.resolve("parametersAfterModification.yml"), destinationDirectory.toPath().resolve("parameters.yml"));
    }


    @Test
    public void testParameterAddAndModify() throws IOException {


        Parameter parameter = new Parameter(null, "addedParameter", "test", "This is an added Parameter before modification");
        assertThrows(AutomationPackagePerObjectSaveUnsupportedException.class, () -> parameterCollection.save(parameter));


        setPropertiesWriteToFragment(Parameter.ENTITY_NAME, "parameters.yml");
        parameter.setPriority(1);
        parameterCollection.save(parameter);

        assertFilesEqual(expectedFilesPath.resolve("parametersAfterAdd.yml"), destinationDirectory.toPath().resolve("parameters.yml"));

        parameter.setDescription("This is an added Parameter with a new description");
        parameterCollection.save(parameter);

        parameter.setValue(new DynamicValue<>("foo"));
        parameter.setPriority(null);
        parameterCollection.save(parameter);

        assertFilesEqual(expectedFilesPath.resolve("parametersAfterAddAndModification.yml"), destinationDirectory.toPath().resolve("parameters.yml"));
    }

    @Test
    public void testParametersInRootApAreAddedOnlyOnce() throws Exception {
        // SED-4681
        assertEquals(1, parameterCollection.find(Filters.equals("key", "paramInMainAP"), null, null, null, 0).count());
    }

}
