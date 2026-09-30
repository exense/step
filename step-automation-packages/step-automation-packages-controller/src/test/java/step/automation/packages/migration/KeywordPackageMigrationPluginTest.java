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
import step.core.Version;
import step.migration.MigrationManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The second phase must run exactly on the startup whose migration task filled the staging collection,
 * which is the upgrade crossing {@link KeywordPackageMigrationTask#AS_OF_VERSION}. Running it more
 * often means touching the staging collection on every boot for the rest of the installation's life;
 * running it less often leaves the keyword packages staged and their keywords orphaned.
 * <p>
 * Asserted through the framework rule the plugin delegates to, so that these stay the expectations of
 * the keyword package migration rather than a second copy of the rule.
 */
public class KeywordPackageMigrationPluginTest {

    private static boolean migrationRequired(Version previous, Version current) {
        return MigrationManager.isMigrationTaskInScope(
                KeywordPackageMigrationTask.AS_OF_VERSION, previous, current);
    }

    @Test
    public void anUpgradeCrossingTheMigrationVersionRequiresIt() {
        assertTrue(migrationRequired(new Version(3, 30, 4), new Version(3, 31, 0)));
    }

    @Test
    public void anUpgradeJumpingOverTheMigrationVersionRequiresIt() {
        assertTrue(migrationRequired(new Version(3, 29, 0), new Version(3, 32, 1)));
    }

    /** Version tracking was introduced in 3.8.0, so a start log without a version stands for 3.7.0. */
    @Test
    public void anUpgradeFromBeforeVersionTrackingRequiresIt() {
        assertTrue(migrationRequired(new Version(3, 7, 0), new Version(3, 31, 0)));
    }

    /** The regression guard: the upgrade already happened, every later start must skip. */
    @Test
    public void aRestartOnTheMigrationVersionDoesNotRequireIt() {
        assertFalse(migrationRequired(new Version(3, 31, 0), new Version(3, 31, 0)));
    }

    @Test
    public void aLaterUpgradeDoesNotRequireIt() {
        assertFalse(migrationRequired(new Version(3, 31, 0), new Version(3, 32, 0)));
        assertFalse(migrationRequired(new Version(3, 32, 0), new Version(3, 33, 0)));
    }

    @Test
    public void anUpgradeStoppingShortOfTheMigrationVersionDoesNotRequireIt() {
        assertFalse(migrationRequired(new Version(3, 30, 0), new Version(3, 30, 5)));
    }

    /** A downgrade has no second phase either: the task only renames the collection on the way up. */
    @Test
    public void aDowngradeDoesNotRequireIt() {
        assertFalse(migrationRequired(new Version(3, 32, 0), new Version(3, 30, 0)));
    }

    /**
     * The start is recorded before the migration runs, so an administrator correcting the mode and
     * restarting would silently get no migration at all. The message has to say what else to do.
     */
    @Test
    public void theRestartHintNamesTheRecordToDelete() {
        assertTrue(KeywordPackageMigrationPlugin.RESTART_MIGRATION_HINT.contains("controllerlogs"));
        assertTrue(KeywordPackageMigrationPlugin.RESTART_MIGRATION_HINT.contains(
                KeywordPackageMigrationTask.AS_OF_VERSION.toString()));
    }
}
