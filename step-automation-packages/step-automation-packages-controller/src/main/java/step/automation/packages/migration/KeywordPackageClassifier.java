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

import step.attachments.FileResolver;

import java.io.File;
import java.util.function.Predicate;



/**
 * Decides what has to become of a staged keyword package.
 * <p>
 * Kept free of any database and manager dependency so that the rules, which decide whether a package
 * is converted or dropped, can be tested against hand-built packages.
 */
public class KeywordPackageClassifier {

    /**
     * Marks a package imported from the embedded packages folder. Such packages are always located by
     * absolute path and will now be managed by the embedded Automation Packages, so they are deleted rather than migrated in every mode.
     */
    static final String EMBEDDED_PACKAGE_CUSTOM_FIELD = "embeddedPackage";

    private final Predicate<String> fileReadable;

    public KeywordPackageClassifier() {
        this(path -> {
            File file = new File(path);
            return file.exists() && file.canRead();
        });
    }

    /**
     * @param fileReadable decides whether a filesystem location still resolves
     */
    public KeywordPackageClassifier(Predicate<String> fileReadable) {
        this.fileReadable = fileReadable;
    }

    public KeywordPackageBucket classify(StagedKeywordPackage keywordPackage) {
        if (keywordPackage.getCustomField(EMBEDDED_PACKAGE_CUSTOM_FIELD) != null) {
            return KeywordPackageBucket.EMBEDDED;
        }

        String packageLocation = keywordPackage.getPackageLocation();
        if (isBlank(packageLocation) || isUnavailable(packageLocation)) {
            // Nothing to convert and nothing to point an automation package at. Treated as a broken
            // location rather than as an error, so that it is logged and cleaned up like the others.
            return KeywordPackageBucket.ARCHIVE_MISSING;
        }

        // Libraries if set must be valid too
        String librariesLocation = keywordPackage.getPackageLibrariesLocation();
        if (!isBlank(librariesLocation) && isUnavailable(librariesLocation)) {
            return KeywordPackageBucket.ARCHIVE_MISSING;
        }

        return KeywordPackageBucket.MIGRATABLE;
    }

    /**
     * A {@code resource:} reference is taken at face value here. Whether the resource still exists is a
     * database question, and this classifier deliberately has none, so that is checked when the package
     * is converted — where a missing resource leads to the same outcome as a missing file.
     */
    private boolean isUnavailable(String location) {
        return !location.startsWith(FileResolver.RESOURCE_PREFIX) && !fileReadable.test(location);
    }

    private boolean isBlank(String location) {
        return location == null || location.isBlank();
    }
}
