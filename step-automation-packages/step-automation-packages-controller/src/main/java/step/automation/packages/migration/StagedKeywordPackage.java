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

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

import java.util.Map;

/**
 * A keyword package waiting to be replaced, as moved aside by {@link KeywordPackageMigrationTask}.
 * <p>
 * This is a verbatim copy of the {@code functionPackage} document: the migration task moves the
 * documents rather than interpreting them, so this type only has to name the fields the plugin later
 * reads. It keeps its original id, which is what the plugin uses to find the keywords the package
 * deployed through {@code customFields.functionPackageId}.
 * <p>
 * Being an {@link EnricheableObject} carrying the original {@code attributes} is what makes the
 * standard enrichment machinery usable: the plugin can call
 * {@code objectHookRegistry.rebuildContext(context, staged)} followed by
 * {@code getObjectEnricher(context)} and get every registered hook applied — project ownership and
 * anything else a hook contributes — rather than a hand-written copy of one attribute.
 */
public class StagedKeywordPackage extends AbstractOrganizableObject implements EnricheableObject {

    private String packageLocation;
    private String packageLibrariesLocation;
    private Map<String, String> packageAttributes;
    private Map<String, String> tokenSelectionCriteria;
    private boolean executeLocally;

    public String getPackageLocation() {
        return packageLocation;
    }

    public void setPackageLocation(String packageLocation) {
        this.packageLocation = packageLocation;
    }

    public String getPackageLibrariesLocation() {
        return packageLibrariesLocation;
    }

    public void setPackageLibrariesLocation(String packageLibrariesLocation) {
        this.packageLibrariesLocation = packageLibrariesLocation;
    }

    /** Merged onto every keyword at deployment time; becomes the automation package's functionsAttributes. */
    public Map<String, String> getPackageAttributes() {
        return packageAttributes;
    }

    public void setPackageAttributes(Map<String, String> packageAttributes) {
        this.packageAttributes = packageAttributes;
    }

    public Map<String, String> getTokenSelectionCriteria() {
        return tokenSelectionCriteria;
    }

    public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
    }

    public boolean isExecuteLocally() {
        return executeLocally;
    }

    public void setExecuteLocally(boolean executeLocally) {
        this.executeLocally = executeLocally;
    }

    /**
     * @return a description usable in a log line, naming both locations so that an administrator can
     * act on a package that could not be migrated. The libraries are included whenever the package
     * declares them, since they are as capable of being the missing part as the archive is.
     */
    public String describe() {
        StringBuilder description = new StringBuilder("'").append(getAttribute(AbstractOrganizableObject.NAME))
                .append("' (id=").append(getId()).append(", location=").append(packageLocation);
        if (packageLibrariesLocation != null && !packageLibrariesLocation.isBlank()) {
            description.append(", libraries=").append(packageLibrariesLocation);
        }
        return description.append(")").toString();
    }
}
