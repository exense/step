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

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Selects what the upgrade does with the keyword packages found in the database.
 * <p>
 * Keyword packages are removed in Step 31, so no mode leaves the {@code functionPackage} entities
 * in place: the choice is only about what happens to the keywords they deployed and the resources
 * they own.
 */
public enum KeywordPackageMigrationMode {

    /**
     * Default. The keyword package is replaced by an automation package deployed through the
     * automation package manager, which rebuilds the keywords from the archive. Any reconfiguration
     * applied to a keyword since its package was deployed is therefore lost; {@link #DETACH} is the
     * alternative for installations that cannot accept that.
     */
    MIGRATE,

    /**
     * Only the keyword package entity is removed. Its keywords and the resources they use are left
     * exactly as they are, becoming plain manually managed keywords that nothing will update again.
     * The escape hatch for installations that reconfigured keywords by hand, since {@link #MIGRATE}
     * rebuilds them from the archive and discards those edits.
     */
    DETACH,

    /**
     * The keyword package, its keywords and the resources it owns are all removed. No automation
     * package is created.
     */
    DELETE;

    public static final String PROPERTY_KEY = "keyword.packages.migration.mode";

    /**
     * @return the value to configure this mode with, which is simply the lowercase constant name
     */
    public String getPropertyValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the mode from the raw value of {@value #PROPERTY_KEY}.
     *
     * @param propertyValue the configured value, {@code null} or blank to get the default
     * @return the resolved mode, never {@code null}
     * @throws IllegalArgumentException if the value matches no mode. Failing rather than falling back
     *                                  to the default is deliberate: the default rebuilds keywords
     *                                  from their archive, so a mistyped {@code detach} would discard
     *                                  the manual reconfiguration that mode exists to protect
     */
    public static KeywordPackageMigrationMode parse(String propertyValue) {
        if (propertyValue == null || propertyValue.isBlank()) {
            return MIGRATE;
        }
        String trimmed = propertyValue.trim();
        try {
            // Locale.ROOT rather than the default locale: "migrate" contains an 'i', which a Turkish
            // locale would upper-case to 'İ' and fail to match.
            return valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // valueOf only reports that the constant does not exist, which does not help someone
            // editing step.properties.
            throw new IllegalArgumentException("Unsupported value '" + trimmed + "' for the property "
                    + PROPERTY_KEY + ". Supported values are: "
                    + Arrays.stream(values()).map(KeywordPackageMigrationMode::getPropertyValue)
                    .collect(Collectors.joining(", ")), e);
        }
    }
}
