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
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserialization.AutomationPackagePerObjectSaveUnsupportedException;
import step.functions.Function;
import step.parameter.Parameter;
import step.plugins.functions.types.CompositeFunction;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AutomationPackageKeywordCollectionTest extends AutomationPackageCollectionTestBase {

    private Collection<Function> functionCollection;

    public AutomationPackageKeywordCollectionTest() {
        super();
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        functionCollection = collectionFactory.getCollection(YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME, Function.class);
    }

    @Test
    public void testLoadAllKeywords() throws IOException {
        List<Function> functions = functionCollection.find(Filters.empty(), null, null, null, 100).collect(Collectors.toList());

        assertEquals(4, functions.size());
        Set<String> functionNames = functions.stream().map(f -> f.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.toSet());

        assertTrue(functionNames.contains("NodeAutomation"));
        assertTrue(functionNames.contains("JMeter keyword from automation package"));
        assertTrue(functionNames.contains("Composite keyword from AP"));
        assertTrue(functionNames.contains("GeneralScript keyword from AP"));
    }

    @Test
    public void testModifyCompositeKeyword() throws IOException {
        Optional<Function> optionalFunction = functionCollection.find(Filters.equals("attributes.name", "Composite keyword from AP"), null, null, null, 100).findFirst();
        assertTrue(optionalFunction.isPresent());

        CompositeFunction compositeFunction = (CompositeFunction) optionalFunction.get();

        Echo echo = (Echo) compositeFunction.getPlan().getRoot().getChildren().getFirst();
        echo.setText(new DynamicValue<>("Modified Echo"));

        setPropertiesWriteToFragment(YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME, "keywords.yml");
        functionCollection.save(compositeFunction);

        assertFilesEqual(expectedFilesPath.resolve("keywordsAfterCompositeModification.yml"), destinationDirectory.toPath().resolve("keywords.yml"));
    }

}
