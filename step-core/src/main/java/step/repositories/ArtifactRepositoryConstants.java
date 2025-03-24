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
package step.repositories;

public class ArtifactRepositoryConstants {
    // Repositories
    public static final String MAVEN_REPO_ID = "Artifact";
    public static final String RESOURCE_REPO_ID = "ResourceArtifact";

    // Parameters in Artifact Repository
    public static final String ARTIFACT_PARAM_ARTIFACT_ID = "artifactId";
    public static final String ARTIFACT_PARAM_VERSION = "version";
    public static final String ARTIFACT_PARAM_GROUP_ID = "groupId";
    public static final String ARTIFACT_PARAM_CLASSIFIER = "classifier";

    public static final String ARTIFACT_PARAM_MAVEN_SETTINGS = "mavenSettings";
    public static final String ARTIFACT_PARAM_MAVEN_SETTINGS_DEFAULT = "default";
    public static final String MAVEN_SETTINGS_PREFIX = "maven_settings_";

    public static final String PARAM_WRAP_PLANS_INTO_TEST_SET = "wrapPlans";
    public static final String PARAM_THREAD_NUMBER = "threads";
    public static final String PARAM_INCLUDE_PLANS = "includePlans";
    public static final String PARAM_EXCLUDE_PLANS = "excludePlans";
    public static final String PARAM_INCLUDE_CATEGORIES = "includeCategories";
    public static final String PARAM_EXCLUDE_CATEGORIES = "excludeCategories";

    // Parameters in Resource Artifact Repository
    public static final String RESOURCE_PARAM_RESOURCE_ID = "resourceId";
}
