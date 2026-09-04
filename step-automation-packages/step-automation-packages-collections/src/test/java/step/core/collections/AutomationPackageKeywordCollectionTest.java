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

import ch.exense.commons.app.Configuration;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.Echo;
import step.attachments.FileResolver;
import step.automation.packages.ApResourceProvider;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.Function;
import step.functions.type.FunctionTypeConfiguration;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.node.NodeFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    /**
     * The files a keyword refers to are held as apResource:local: references while the package is
     * open, so that they are validated against the package root and resolved like the files of a
     * deployed package. See AutomationPackageLocalResourceMapper.
     */
    @Test
    public void testKeywordResourcesAreReadAsLocalApResources() throws IOException {
        GeneralScriptFunction keyword = (GeneralScriptFunction) findKeyword("GeneralScript keyword from AP");

        assertEquals("apResource:local:jsProject/jsSample.js", keyword.getScriptFile().get());
        assertEquals("apResource:local:lib/fakeLib.jar", keyword.getLibrariesFile().get());
    }

    /**
     * ... and the descriptor keeps the relative path it was authored with: the reference form is
     * in-memory only, and this is the round trip that proves it. Asserted on the written file rather
     * than through a re-read, since it is the YAML itself that the user owns.
     */
    @Test
    public void testKeywordResourcesAreWrittenBackAsRelativePaths() throws IOException {
        GeneralScriptFunction keyword = (GeneralScriptFunction) findKeyword("GeneralScript keyword from AP");
        keyword.getAttributes().put(AbstractOrganizableObject.NAME, "Renamed GeneralScript keyword");

        functionCollection.save(keyword);

        String writtenKeywords = Files.readString(destinationDirectory.toPath().resolve("keywords.yml"));
        assertTrue(writtenKeywords, writtenKeywords.contains("scriptFile: \"jsProject/jsSample.js\""));
        assertTrue(writtenKeywords, writtenKeywords.contains("librariesFile: \"lib/fakeLib.jar\""));
        assertFalse(writtenKeywords, writtenKeywords.contains("apResource:"));
        // and the entity is left as it was, for the rest of the editing session
        assertEquals("apResource:local:jsProject/jsSample.js", keyword.getScriptFile().get());
    }

    /**
     * Every keyword type goes through the same mapper, so none of them holds a plain path any more, and
     * each maps its own fields back - see Yaml*Function.setDeclaredFieldsFromObject.
     */
    @Test
    public void testResourcesOfEveryKeywordTypeRoundTrip() throws IOException {
        JMeterFunction jmeter = (JMeterFunction) findKeyword("JMeter keyword from automation package");
        NodeFunction node = (NodeFunction) findKeyword("NodeAutomation");

        assertEquals("apResource:local:jmeterProject1/jmeterProject1.xml", jmeter.getJmeterTestplan().get());
        assertEquals("apResource:local:nodeProject/nodeSample.ts", node.getJsFile().get());

        functionCollection.save(jmeter);
        functionCollection.save(node);

        String writtenKeywords = Files.readString(destinationDirectory.toPath().resolve("keywords.yml"));
        assertTrue(writtenKeywords, writtenKeywords.contains("jmeterTestplan: \"jmeterProject1/jmeterProject1.xml\""));
        assertTrue(writtenKeywords, writtenKeywords.contains("jsfile: \"nodeProject/nodeSample.ts\""));
        assertFalse(writtenKeywords, writtenKeywords.contains("apResource:"));
    }

    /**
     * A keyword created from the editor gets its script inside the automation package, and the descriptor
     * the plain relative path - the whole chain, from the keyword type creating the file to the YAML it
     * ends up in. On a Step server the same call creates a Step resource instead, which here would leave
     * the package without its script and the descriptor with a resource id.
     */
    @Test
    public void testKeywordCreatedFromTheEditorGetsItsScriptInThePackage() throws Exception {
        GeneralScriptFunction keyword = new GeneralScriptFunction();
        keyword.addAttribute(AbstractOrganizableObject.NAME, "New Groovy keyword");
        keyword.setScriptLanguage(new DynamicValue<>("groovy"));
        keyword.setScriptFile(new DynamicValue<>(""));

        new EditorGeneralScriptFunctionType(destinationDirectory.toPath()).setupFunction(keyword);
        setPropertiesWriteToFragment(YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME, "keywords.yml");
        functionCollection.save(keyword);

        assertEquals("apResource:local:groovy/New_Groovy_keyword.groovy", keyword.getScriptFile().get());
        assertTrue(new File(destinationDirectory, "groovy/New_Groovy_keyword.groovy").exists());

        String writtenKeywords = Files.readString(destinationDirectory.toPath().resolve("keywords.yml"));
        assertTrue(writtenKeywords, writtenKeywords.contains("scriptFile: \"groovy/New_Groovy_keyword.groovy\""));
        assertFalse(writtenKeywords, writtenKeywords.contains("apResource:"));
        assertFalse(writtenKeywords, writtenKeywords.contains("resource:"));
    }

    /**
     * The keyword type as the editor wires it: an {@code ApResourceProvider} reporting the open package as
     * editable is what tells it to create the script there. The dependencies are injected through
     * protected setters, hence the subclass.
     */
    private static class EditorGeneralScriptFunctionType extends GeneralScriptFunctionType {
        EditorGeneralScriptFunctionType(Path automationPackageRoot) {
            super(new Configuration());
            FileResolver fileResolver = new FileResolver(null);
            fileResolver.setApResourceProvider(new ApResourceProvider() {
                @Override
                public File resolve(String apId, String relativePath) {
                    return automationPackageRoot.resolve(relativePath).toFile();
                }

                @Override
                public Path getEditableRoot() {
                    return automationPackageRoot;
                }
            });
            setFunctionTypeConfiguration(new FunctionTypeConfiguration());
            setFileResolver(fileResolver);
            setObjectHookRegistry(new ObjectHookRegistry());
        }
    }

    private Function findKeyword(String name) {
        Optional<Function> keyword = functionCollection
            .find(Filters.equals("attributes.name", name), null, null, null, 100).findFirst();
        assertTrue("keyword not found: " + name, keyword.isPresent());
        return keyword.get();
    }

    @Test
    public void testRenameCompositeKeyword() throws IOException {
        Optional<Function> optionalFunction = functionCollection.find(Filters.equals("attributes.name", "Composite keyword from AP"), null, null, null, 100).findFirst();
        assertTrue(optionalFunction.isPresent());

        CompositeFunction compositeFunction = (CompositeFunction) optionalFunction.get();

        compositeFunction.getAttributes().put(AbstractOrganizableObject.NAME, "Renamed Composite Keyword");

        functionCollection.save(compositeFunction);

        assertFilesEqual(expectedFilesPath.resolve("keywordsAfterCompositeRenamed.yml"), destinationDirectory.toPath().resolve("keywords.yml"));
    }

}
