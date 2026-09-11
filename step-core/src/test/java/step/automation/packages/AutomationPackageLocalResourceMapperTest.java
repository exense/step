/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.automation.packages;

import org.junit.Test;
import step.core.dynamicbeans.DynamicValue;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The reference format of the automation package editor: what the descriptor holds as a plain
 * relative path is held in memory as {@code apResource:local:<path>}, and never the other way round.
 * <p>
 * The last tests are about {@link AutomationPackageResourceMapper} instead, where the two differ:
 * the deploy mapper refuses an authored {@code apResource:} reference, while this one lets it
 * through and normalises it away on the next save.
 */
public class AutomationPackageLocalResourceMapperTest {

    private final AutomationPackageLocalResourceMapper mapper = new AutomationPackageLocalResourceMapper();

    @Test
    public void prefixesARelativePath() {
        assertEquals("apResource:local:scripts/kw.groovy", apply("scripts/kw.groovy"));
    }

    /**
     * Normalising here is what makes the reference addressable: the browser, the resolver and the
     * descriptor all end up talking about the same path.
     */
    @Test
    public void normalisesThePath() {
        assertEquals("apResource:local:scripts/kw.groovy", apply("./scripts/kw.groovy"));
        assertEquals("apResource:local:scripts/kw.groovy", apply("scripts\\kw.groovy"));
        assertEquals("apResource:local:scripts/kw.groovy", apply("/scripts/kw.groovy"));
        assertEquals("apResource:local:scripts/kw.groovy", apply("scripts/../scripts/kw.groovy"));
    }

    /**
     * An automation package is self-contained - which a plain relative path, resolved through the
     * unprefixed root, never checked.
     */
    @Test
    public void rejectsAPathEscapingTheAutomationPackage() {
        assertThrows(IllegalArgumentException.class, () -> apply("../outside.csv"));
    }

    /**
     * The editor is deliberately more permissive than the deploy mapper, which refuses a hand-written
     * {@code apResource:} reference: refusing here would mean refusing to open the package, leaving
     * the user nowhere to fix it. The reference is written back as a plain path on the next save.
     */
    @Test
    public void leavesAnAlreadyPrefixedReferenceUntouched() {
        // a hand written resource: keeps working, and the mapping stays idempotent
        assertEquals("resource:66c1f0f0f0f0f0f0f0f0f0f0", apply("resource:66c1f0f0f0f0f0f0f0f0f0f0"));
        assertEquals("apResource:local:data/pool.csv", apply("apResource:local:data/pool.csv"));
    }

    @Test
    public void reportsAnAbsentReferenceAsNull() {
        assertNull(apply(null));
        assertNull(apply(""));
    }

    @Test
    public void bothApplyMethodsAgree() {
        assertEquals(apply("data/pool.csv"),
            mapper.applyUniqueResourceReference("data/pool.csv", context(mapper)));
    }

    /**
     * An {@code apResource:} reference is what the deploy mapper <i>produces</i>, so a descriptor
     * holding one was written by hand - the editor writes the plain relative path back, and a
     * descriptor is read into fresh entities every time, so this is never a second pass over an
     * already mapped reference. Both the editor form and a reference to another package are refused
     * by the same branch, with a message telling the author what to write instead.
     */
    @Test
    public void theUploaderRefusesAnAuthoredApResourceReference() {
        AutomationPackageResourceMapper uploader = new AutomationPackageResourceMapper();

        for (String authored : new String[]{"apResource:local:data/pool.csv",
            "apResource:66c1f0f0f0f0f0f0f0f0f0f0:data/pool.csv"}) {
            RuntimeException e = assertThrows(authored, RuntimeException.class,
                () -> uploader.applyResourceReference(authored, context(uploader)));

            assertTrue(e.getMessage(), e.getMessage().contains(authored));
            assertTrue(e.getMessage(), e.getMessage().contains("cannot be written in a descriptor"));
        }
    }

    /**
     * A {@code resource:} reference is an authored form rather than a leftover, so the deploy mapper
     * has to let it through: the schema declares the file of a data source as
     * {@code oneOf: [string, {id: <string>}]}, and {@code YamlResourceReference.toDynamicValue} turns
     * the {@code {id: ...}} form into {@code resource:<id>} before the mapper sees it. Treating it as
     * a path would look for {@code resource:<id>} in the archive - or fail to build a path from it at
     * all, on a file system that has no colons.
     */
    @Test
    public void theUploaderLeavesAStepResourceUntouched() {
        AutomationPackageResourceMapper uploader = new AutomationPackageResourceMapper();

        assertEquals("resource:66c1f0f0f0f0f0f0f0f0f0f0",
            uploader.applyResourceReference("resource:66c1f0f0f0f0f0f0f0f0f0f0", context(uploader)));
    }

    /**
     * The way back, called by the {@code setDeclaredFieldsFromObject} of every keyword plugin: only the
     * editor form is unwrapped, everything else is what the descriptor already held.
     */
    @Test
    public void writesTheEditorFormBackAsThePathItWasAuthoredWith() {
        assertEquals("scripts/kw.groovy",
            AutomationPackageLocalResourceMapper.toDescriptorReference("apResource:local:scripts/kw.groovy"));
        assertEquals("resource:66c1f0f0f0f0f0f0f0f0f0f0",
            AutomationPackageLocalResourceMapper.toDescriptorReference("resource:66c1f0f0f0f0f0f0f0f0f0f0"));
        assertEquals("scripts/kw.groovy",
            AutomationPackageLocalResourceMapper.toDescriptorReference("scripts/kw.groovy"));
    }

    /**
     * Null in, null out - and nothing else maps to null. Callers rely on it to keep working on the
     * result: {@code YamlK6Function.descriptorPath} excludes a null value of its own and then maps the
     * separators of whatever comes back.
     */
    @Test
    public void mapsOnlyAnAbsentReferenceToNull() {
        assertNull(AutomationPackageLocalResourceMapper.toDescriptorReference((String) null));
        assertNotNull(AutomationPackageLocalResourceMapper.toDescriptorReference("apResource:local:kw.groovy"));
        assertNotNull(AutomationPackageLocalResourceMapper.toDescriptorReference(""));
    }

    /**
     * The {@link DynamicValue} overload turns an absent reference into an <b>empty</b> value rather
     * than none: a yaml model serialized with {@code NON_DEFAULT} compares what it holds against its
     * own default through {@link DynamicValue#equals}, which reads both values, so a value holding
     * nothing at all cannot be written.
     */
    @Test
    public void writesAnAbsentDynamicReferenceAsAnEmptyValue() {
        assertEquals("", AutomationPackageLocalResourceMapper.toDescriptorReference((DynamicValue<String>) null).getValue());
        assertEquals("", AutomationPackageLocalResourceMapper.toDescriptorReference(new DynamicValue<String>()).getValue());
        assertEquals("scripts/kw.groovy", AutomationPackageLocalResourceMapper
            .toDescriptorReference(new DynamicValue<>("apResource:local:scripts/kw.groovy")).getValue());
    }

    /**
     * A dynamic expression holds no path to map back, so it is returned as it is rather than being
     * flattened into the value it last evaluated to.
     */
    @Test
    public void leavesADynamicExpressionAlone() {
        DynamicValue<String> expression = new DynamicValue<>("scriptFile", "");

        assertSame(expression, AutomationPackageLocalResourceMapper.toDescriptorReference(expression));
    }

    private String apply(String reference) {
        return mapper.applyResourceReference(reference, context(mapper));
    }

    /**
     * The mapper reads nothing off the context, but the signature requires one.
     */
    private static StagingAutomationPackageContext context(AutomationPackageResourceMapper uploader) {
        return new StagingAutomationPackageContext(uploader, new AutomationPackage(),
            AutomationPackageOperationMode.LOCAL, null, null, null, null, null, new HashMap<>());
    }
}
