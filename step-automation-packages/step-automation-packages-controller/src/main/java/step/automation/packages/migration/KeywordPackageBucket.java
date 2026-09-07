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

/**
 * What has to become of a keyword package. Only the distinctions the migration acts on are modelled:
 * whether the archive sits in a resource or on the filesystem is settled at deployment time, from the
 * location itself.
 */
public enum KeywordPackageBucket {

    /**
     * Imported from the embedded packages folder. Always located by absolute path and never owning a
     * resource, so there is nothing to convert: the package and its keywords are removed and the
     * embedded automation package feature recreates them. Reported to nobody, since an administrator
     * has nothing to act on.
     */
    EMBEDDED,

    /** Everything it needs is reachable, so it is replaced by an automation package. */
    MIGRATABLE,

    /**
     * Something the package needs is not there: its archive, or the libraries it declares. Libraries
     * count because the keywords were built against them, so a package converted without them would be
     * one that cannot run. Nothing can execute these keywords today either way, so the package is
     * logged with its location and removed without a replacement.
     */
    ARCHIVE_MISSING
}
