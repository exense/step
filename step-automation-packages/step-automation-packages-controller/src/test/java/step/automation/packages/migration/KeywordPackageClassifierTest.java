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
package step.automation.packages.migration;

import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.*;
import static step.automation.packages.migration.KeywordPackageClassifier.EMBEDDED_PACKAGE_CUSTOM_FIELD;

public class KeywordPackageClassifierTest {

    private static final String READABLE_PATH = "/opt/step/keywords/payments.jar";
    private static final String READABLE_LIBRARIES_PATH = "/opt/step/keywords/payments-libs.zip";
    private static final String MISSING_PATH = "/opt/step/keywords/gone.jar";

    private final KeywordPackageClassifier classifier =
            new KeywordPackageClassifier(Set.of(READABLE_PATH, READABLE_LIBRARIES_PATH)::contains);

    @Test
    public void resourceBackedPackagesAreMigratable() {
        KeywordPackageBucket bucket = classifier.classify(keywordPackage("resource:65120000000000000000000a"));

        assertEquals(KeywordPackageBucket.MIGRATABLE, bucket);
    }

    @Test
    public void readablePathsAreMigratable() {
        KeywordPackageBucket bucket = classifier.classify(keywordPackage(READABLE_PATH));

        assertEquals(KeywordPackageBucket.MIGRATABLE, bucket);
    }

    @Test
    public void missingPathsAreDroppedRatherThanMigrated() {
        KeywordPackageBucket bucket = classifier.classify(keywordPackage(MISSING_PATH));

        assertEquals(KeywordPackageBucket.ARCHIVE_MISSING, bucket);
    }

    @Test
    public void aBlankLocationIsTreatedAsAMissingPath() {
        assertEquals(KeywordPackageBucket.ARCHIVE_MISSING, classifier.classify(keywordPackage(null)));
        assertEquals(KeywordPackageBucket.ARCHIVE_MISSING, classifier.classify(keywordPackage("  ")));
    }

    /**
     * Libraries are optional, but once declared they are as mandatory as the archive: the keywords were
     * built against them, so converting without them would produce a package that cannot run.
     */
    @Test
    public void aPackageWhoseLibrariesAreMissingIsDroppedEvenWhenItsArchiveIsReadable() {
        StagedKeywordPackage keywordPackage = keywordPackage(READABLE_PATH);
        keywordPackage.setPackageLibrariesLocation(MISSING_PATH);

        assertEquals(KeywordPackageBucket.ARCHIVE_MISSING, classifier.classify(keywordPackage));
    }

    @Test
    public void aResourceBackedPackageWhoseLibrariesPathIsMissingIsAlsoDropped() {
        StagedKeywordPackage keywordPackage = keywordPackage("resource:65120000000000000000000a");
        keywordPackage.setPackageLibrariesLocation(MISSING_PATH);

        assertEquals(KeywordPackageBucket.ARCHIVE_MISSING, classifier.classify(keywordPackage));
    }

    @Test
    public void readableLibrariesKeepThePackageMigratable() {
        StagedKeywordPackage keywordPackage = keywordPackage(READABLE_PATH);
        keywordPackage.setPackageLibrariesLocation(READABLE_LIBRARIES_PATH);

        assertEquals(KeywordPackageBucket.MIGRATABLE, classifier.classify(keywordPackage));
    }

    /**
     * The two locations were always set independently, so a resource archive next to a filesystem
     * libraries path is a legal combination that has to keep working.
     */
    @Test
    public void aResourceArchiveMayBePairedWithAReadableLibrariesPath() {
        StagedKeywordPackage keywordPackage = keywordPackage("resource:65120000000000000000000a");
        keywordPackage.setPackageLibrariesLocation(READABLE_LIBRARIES_PATH);

        assertEquals(KeywordPackageBucket.MIGRATABLE, classifier.classify(keywordPackage));
    }

    /**
     * A resource reference cannot be verified here: whether it still exists is a database question,
     * answered during the conversion instead.
     */
    @Test
    public void resourceReferencedLibrariesAreTakenAtFaceValue() {
        StagedKeywordPackage keywordPackage = keywordPackage("resource:65120000000000000000000a");
        keywordPackage.setPackageLibrariesLocation("resource:65120000000000000000000b");

        assertEquals(KeywordPackageBucket.MIGRATABLE, classifier.classify(keywordPackage));
    }

    /**
     * Embedded packages win over every other rule: they are always located by absolute path, so
     * without this precedence a readable embedded package would be converted instead of deleted, and
     * would then collide with the automation package the embedded importer recreates.
     */
    @Test
    public void embeddedPackagesAreNeverMigratedEvenWhenTheirFileIsReadable() {
        StagedKeywordPackage keywordPackage = keywordPackage(READABLE_PATH);
        keywordPackage.addCustomField(EMBEDDED_PACKAGE_CUSTOM_FIELD, new File(READABLE_PATH));

        KeywordPackageBucket bucket = classifier.classify(keywordPackage);

        assertEquals(KeywordPackageBucket.EMBEDDED, bucket);
    }

    @Test
    public void theDescriptionNamesTheLibrariesWhenThePackageDeclaresThem() {
        StagedKeywordPackage keywordPackage = keywordPackage(READABLE_PATH);
        keywordPackage.setPackageLibrariesLocation(MISSING_PATH);

        String description = keywordPackage.describe();

        assertTrue(description, description.contains(MISSING_PATH));
        assertTrue(description, description.contains("payments"));
    }

    private StagedKeywordPackage keywordPackage(String packageLocation) {
        StagedKeywordPackage keywordPackage = new StagedKeywordPackage();
        keywordPackage.addAttribute(AbstractOrganizableObject.NAME, "payments");
        keywordPackage.setPackageLocation(packageLocation);
        return keywordPackage;
    }
}
