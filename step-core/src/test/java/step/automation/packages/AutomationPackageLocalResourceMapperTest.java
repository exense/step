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
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The reference format of the automation package editor: what the descriptor holds as a plain
 * relative path is held in memory as {@code apResource:local:<path>}, and never the other way round -
 * the deployed form is built by {@link AutomationPackageResourceMapper}, which must refuse an
 * editor-local reference outright.
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
     * The editor form is in-memory only: the descriptor holds the relative path, and
     * {@code AutomationPackageYamlFragmentManager.save} strips the prefix before writing. One reaching
     * the deploy path means an entity was staged straight out of the editor - which would otherwise be
     * returned untouched and deployed as a reference nothing can resolve.
     */
    @Test
    public void theUploaderRefusesAnEditorLocalReference() {
        AutomationPackageResourceMapper uploader = new AutomationPackageResourceMapper();

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> uploader.applyResourceReference("apResource:local:data/pool.csv", context(uploader)));

        assertTrue(e.getMessage(), e.getMessage().contains("cannot be deployed"));
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
