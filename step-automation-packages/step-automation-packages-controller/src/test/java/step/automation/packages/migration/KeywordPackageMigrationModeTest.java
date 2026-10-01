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

import static org.junit.Assert.*;

public class KeywordPackageMigrationModeTest {

    @Test
    public void migrateIsTheDefault() {
        assertEquals(KeywordPackageMigrationMode.MIGRATE, KeywordPackageMigrationMode.parse(null));
        assertEquals(KeywordPackageMigrationMode.MIGRATE, KeywordPackageMigrationMode.parse(""));
        assertEquals(KeywordPackageMigrationMode.MIGRATE, KeywordPackageMigrationMode.parse("   "));
    }

    @Test
    public void everyModeIsAddressableByItsPropertyValue() {
        for (KeywordPackageMigrationMode mode : KeywordPackageMigrationMode.values()) {
            assertEquals(mode, KeywordPackageMigrationMode.parse(mode.getPropertyValue()));
        }
    }

    @Test
    public void parsingIsCaseInsensitiveAndTrimmed() {
        assertEquals(KeywordPackageMigrationMode.DETACH, KeywordPackageMigrationMode.parse("DETACH"));
        assertEquals(KeywordPackageMigrationMode.DETACH, KeywordPackageMigrationMode.parse(" detach "));
        assertEquals(KeywordPackageMigrationMode.DELETE, KeywordPackageMigrationMode.parse("Delete"));
    }

    /**
     * A typo must not silently fall back to the default, which rebuilds keywords and discards any
     * manual reconfiguration: an administrator who mistypes {@code detach} would lose the very edits
     * that mode exists to protect.
     */
    @Test
    public void anUnknownValueFailsLoudlyAndListsTheSupportedOnes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> KeywordPackageMigrationMode.parse("de-tach"));

        assertTrue(exception.getMessage().contains(KeywordPackageMigrationMode.PROPERTY_KEY));
        assertTrue(exception.getMessage().contains("migrate"));
        assertTrue(exception.getMessage().contains("detach"));
    }
}
