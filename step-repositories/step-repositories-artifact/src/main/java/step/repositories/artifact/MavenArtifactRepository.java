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
package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import step.automation.packages.AutomationPackageManager;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.objectenricher.ObjectPredicate;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.ResourceManager;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static step.automation.packages.AutomationPackagePlugin.*;

public class MavenArtifactRepository extends AbstractArtifactRepository {

    public static final String MAVEN_SETTINGS_PREFIX = ArtifactRepositoryConstants.MAVEN_SETTINGS_PREFIX;

    private final ControllerSettingAccessor controllerSettingAccessor;
    private final File localRepository;
    private final Duration maxAge;
    private final Duration cleanupFrequency;

    public MavenArtifactRepository(AutomationPackageManager manager, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor, Configuration configuration,
                                   ControllerSettingAccessor controllerSettingAccessor, ResourceManager resourceManager) {
        super(Set.of(ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID, ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID, ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION), manager, functionTypeRegistry, functionAccessor, resourceManager);
        localRepository = configuration.getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER));
        this.controllerSettingAccessor = controllerSettingAccessor;
        maxAge = Duration.ofMinutes(configuration.getPropertyAsLong(CONFIGURATION_MAVEN_MAX_AGE, DEFAULT_MAVEN_MAX_AGE));
        cleanupFrequency = Duration.ofMinutes(configuration.getPropertyAsLong(CONFIGURATION_MAVEN_CLEANUP_FREQUENCY, DEFAULT_MAVEN_CLEANUP_FREQUENCY));
    }

    private String getMavenSettingsXml(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
        // Priority 1: the explicit parameter in repository params
        String mavenSettingsPostfix = repositoryParameters.get(ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS);
        if (mavenSettingsPostfix == null) {
                mavenSettingsPostfix = ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS_DEFAULT;
        }

        String mavenSettingsId = MAVEN_SETTINGS_PREFIX + mavenSettingsPostfix;
        ControllerSetting controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettingsId);

        if (controllerSetting == null) {
            AbstractArtifactRepository.logger.warn("No settings found for \"" + mavenSettingsId + "\", using empty settings instead.");
            controllerSettingAccessor.updateOrCreateSetting(mavenSettingsId, ArtifactRepositoryConstants.MAVEN_EMPTY_SETTINGS);
            controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettingsId);
        }
        return controllerSetting == null ? null : controllerSetting.getValue();
    }

    @Override
    public File getArtifact(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
        String settingsXml = getMavenSettingsXml(repositoryParameters, objectPredicate);
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(settingsXml, localRepository, maxAge, cleanupFrequency);
            String artifactId = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID);
            String version = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION);
            String groupId = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID);
            String classifier = repositoryParameters.get(ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER);
            String type = repositoryParameters.get(ArtifactRepositoryConstants.ARTIFACT_PARAM_TYPE);
            return mavenArtifactClient.getArtifact(new DefaultArtifact(groupId, artifactId, classifier, type == null || type.isEmpty() ? "jar" : type, version)).artifactFile;
        } catch (SettingsBuildingException | ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String resolveArtifactName(Map<String, String> repositoryParameters) {
        return AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID);
    }

}
