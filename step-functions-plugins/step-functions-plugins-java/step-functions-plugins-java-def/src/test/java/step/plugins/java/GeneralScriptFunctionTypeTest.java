/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
package step.plugins.java;

import ch.exense.commons.app.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.attachments.FileResolver;
import step.automation.packages.ApResourceProvider;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.type.FunctionTypeConfiguration;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Setup and cloning of a script keyword <b>in the automation package editor</b>, where the script must be
 * created inside the package being edited instead of as a Step resource - a resource would live outside
 * the package, in the IDE in a temporary directory, and leave the descriptor with a reference that means
 * nothing anywhere else.
 * <p>
 * The mode is read from the {@link FileResolver}, which is the only dependency of a function type that
 * knows about automation packages: an {@link ApResourceProvider} reporting an editable root <i>is</i> the
 * editor.
 */
public class GeneralScriptFunctionTypeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path apRoot;

    @Before
    public void setUp() throws IOException {
        apRoot = tmp.newFolder("my-ap").toPath();
    }

    /**
     * @param editableRoot the automation package open in the editor, or null for a Step server
     */
    private GeneralScriptFunctionType functionType(Path editableRoot, ResourceManager resourceManager) {
        FileResolver fileResolver = new FileResolver(resourceManager);
        fileResolver.setUnprefixedRoot(apRoot);
        if (editableRoot != null) {
            fileResolver.setApResourceProvider(new ApResourceProvider() {
                @Override
                public File resolve(String apId, String relativePath) {
                    return editableRoot.resolve(relativePath).toFile();
                }

                @Override
                public Path getEditableRoot() {
                    return editableRoot;
                }
            });
        }
        return new TestGeneralScriptFunctionType(fileResolver);
    }

    /**
     * The dependencies a function type receives from {@code FunctionTypeRegistryImpl} are injected through
     * protected setters, so they can only be given from a subclass - the grid and the handler package
     * registration of {@code init()} play no part here.
     */
    private static class TestGeneralScriptFunctionType extends GeneralScriptFunctionType {
        TestGeneralScriptFunctionType(FileResolver fileResolver) {
            super(new Configuration());
            setFunctionTypeConfiguration(new FunctionTypeConfiguration());
            setFileResolver(fileResolver);
            setObjectHookRegistry(new ObjectHookRegistry());
        }
    }

    private GeneralScriptFunctionType editorFunctionType() {
        return functionType(apRoot, null);
    }

    private static GeneralScriptFunction groovyKeyword(String name, String scriptFile) {
        GeneralScriptFunction function = new GeneralScriptFunction();
        function.addAttribute(AbstractOrganizableObject.NAME, name);
        function.setScriptLanguage(new DynamicValue<>("groovy"));
        function.setScriptFile(new DynamicValue<>(scriptFile));
        return function;
    }

    @Test
    public void createsTheScriptInTheAutomationPackage() throws Exception {
        GeneralScriptFunction function = groovyKeyword("My Keyword", "");

        editorFunctionType().setupFunction(function);

        assertEquals("apResource:local:groovy/My_Keyword.groovy", function.getScriptFile().get());
        // the template ships with this plugin, so the editor doesn't create an empty script even though
        // it has no controller directory to take one from
        assertTrue(Files.readString(apRoot.resolve("groovy/My_Keyword.groovy")).contains("context.setPayloadJson"));
    }

    /**
     * One directory per language, so that a package's sources are sorted by what they are - and so that
     * nothing generated lands in {@code keywords}, which is where the YAML fragment of a keyword goes.
     */
    @Test
    public void placesTheScriptInTheDirectoryOfItsLanguage() throws Exception {
        GeneralScriptFunction function = groovyKeyword("Kw", "");
        function.setScriptLanguage(new DynamicValue<>("javascript"));

        editorFunctionType().setupFunction(function);

        assertEquals("apResource:local:javascript/Kw.js", function.getScriptFile().get());
        // the content, not just the file: a template missing from the jar creates an empty script and
        // says so only in a log line, which an existence check would not notice
        assertTrue(Files.readString(apRoot.resolve("javascript/Kw.js")).contains("context.setPayloadJson"));
        assertFalse(Files.exists(apRoot.resolve("keywords")));
    }

    /**
     * The templates ship with this plugin because the IDE has no controller directory to read one from,
     * so nothing but this test stands between a language named here and a keyword created empty. It
     * enumerates the mapping rather than listing languages, so a language added to it is covered by
     * the same assertion.
     */
    @Test
    public void bundlesTheTemplateOfEveryLanguageThatNamesOne() throws Exception {
        GeneralScriptFunctionType functionType = editorFunctionType();

        for (Map.Entry<String, String> language : GeneralScriptFunctionType.TEMPLATE_BY_LANGUAGE.entrySet()) {
            try (InputStream template = functionType.getTemplateFileInputStream(language.getValue())) {
                assertNotNull(language.toString(), template);
                assertFalse(language.toString(), new String(template.readAllBytes(), StandardCharsets.UTF_8).isBlank());
            }
        }
    }

    @Test
    public void doesNotOverwriteAnExistingScript() throws Exception {
        Files.createDirectories(apRoot.resolve("groovy"));
        Files.writeString(apRoot.resolve("groovy/Kw.groovy"), "the user's script");

        GeneralScriptFunction function = groovyKeyword("Kw", "");
        editorFunctionType().setupFunction(function);

        assertEquals("apResource:local:groovy/Kw_2.groovy", function.getScriptFile().get());
        assertEquals("the user's script", Files.readString(apRoot.resolve("groovy/Kw.groovy")));
    }

    @Test
    public void keepsThePathTheUserProvided() throws Exception {
        Files.createDirectories(apRoot.resolve("scripts"));
        Files.writeString(apRoot.resolve("scripts/existing.groovy"), "the user's script");

        GeneralScriptFunction function = groovyKeyword("Kw", "scripts/existing.groovy");
        editorFunctionType().setupFunction(function);

        assertEquals("apResource:local:scripts/existing.groovy", function.getScriptFile().get());
        assertEquals("the user's script", Files.readString(apRoot.resolve("scripts/existing.groovy")));
    }

    @Test
    public void createsThePathTheUserProvidedIfItIsMissing() throws Exception {
        GeneralScriptFunction function = groovyKeyword("Kw", "scripts/new.groovy");

        editorFunctionType().setupFunction(function);

        assertEquals("apResource:local:scripts/new.groovy", function.getScriptFile().get());
        assertTrue(Files.readString(apRoot.resolve("scripts/new.groovy")).contains("context.setPayloadJson"));
    }

    @Test
    public void leavesAStepResourceReferenceAlone() throws Exception {
        GeneralScriptFunction function = groovyKeyword("Kw", "resource:507f1f77bcf86cd799439011");

        editorFunctionType().setupFunction(function);

        assertEquals("resource:507f1f77bcf86cd799439011", function.getScriptFile().get());
        assertFalse(Files.exists(apRoot.resolve("groovy")));
    }

    @Test
    public void copiesTheScriptNextToItsSource() throws Exception {
        Files.createDirectories(apRoot.resolve("scripts"));
        Files.writeString(apRoot.resolve("scripts/Kw.groovy"), "the source script");
        GeneralScriptFunction function = groovyKeyword("Kw", "apResource:local:scripts/Kw.groovy");

        GeneralScriptFunction copy = editorFunctionType().copyFunction(function);

        // next to the source, so the layout the user chose survives the copy, and under the copy's own name
        assertEquals("apResource:local:scripts/Kw_Copy.groovy", copy.getScriptFile().get());
        assertEquals("the source script", Files.readString(apRoot.resolve("scripts/Kw_Copy.groovy")));
        assertEquals("the source script", Files.readString(apRoot.resolve("scripts/Kw.groovy")));
    }

    @Test
    public void copiesAKeywordThatHasNoScriptYet() throws Exception {
        GeneralScriptFunction copy = editorFunctionType().copyFunction(groovyKeyword("Kw", ""));

        assertEquals("", copy.getScriptFile().get());
        assertFalse(Files.exists(apRoot.resolve("groovy")));
    }

    /**
     * The template is opened by the caller and handed over, so the setup owns it: a keyword created every
     * time a user clicks would otherwise leave one open stream behind each time - on the distribution's
     * template, or on the script a clone reads. The two paths that never read it are the ones worth
     * pinning, since a stream is easy to close where it is consumed and easy to forget where it is not.
     */
    @Test
    public void closesTheTemplateStreamItIsGiven() throws Exception {
        RecordingInputStream template = new RecordingInputStream("println 'hello'");
        editorFunctionType().setupScriptFileAsResource(groovyKeyword("Kw", ""), template);
        assertTrue("the template of a created script", template.closed);

        RecordingInputStream unread = new RecordingInputStream("println 'hello'");
        editorFunctionType().setupScriptFileAsResource(groovyKeyword("Kw", "resource:507f1f77bcf86cd799439011"), unread);
        assertTrue("the template of a keyword referencing a Step resource", unread.closed);

        Files.createDirectories(apRoot.resolve("scripts"));
        Files.writeString(apRoot.resolve("scripts/existing.groovy"), "the user's script");
        RecordingInputStream skipped = new RecordingInputStream("println 'hello'");
        editorFunctionType().setupScriptFileAsResource(groovyKeyword("Kw", "scripts/existing.groovy"), skipped);
        assertTrue("the template of a keyword whose script exists already", skipped.closed);
    }

    private static class RecordingInputStream extends ByteArrayInputStream {

        private boolean closed;

        RecordingInputStream(String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    public void createsAResourceWhereThereIsNoEditableAutomationPackage() throws Exception {
        ResourceManager resourceManager = new LocalResourceManagerImpl(tmp.newFolder("resources"));
        GeneralScriptFunction function = groovyKeyword("Kw", "");

        functionType(null, resourceManager).setupFunction(function);

        // unchanged server behaviour: the script becomes a Step resource
        assertTrue(function.getScriptFile().get(), FileResolver.isResource(function.getScriptFile().get()));
        assertFalse(Files.exists(apRoot.resolve("groovy")));
    }
}
