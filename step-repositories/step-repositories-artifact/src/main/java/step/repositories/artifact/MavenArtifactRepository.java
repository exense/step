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
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class MavenArtifactRepository extends AbstractArtifactRepository {

    protected static final String PARAM_ARTIFACT_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID;
    protected static final String PARAM_VERSION = ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION;
    protected static final String PARAM_GROUP_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID;
    protected static final String PARAM_TYPE = ArtifactRepositoryConstants.ARTIFACT_PARAM_TYPE;
    protected static final String PARAM_CLASSIFIER = ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER;

    protected static final String PARAM_MAVEN_SETTINGS = ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS;

    public static final String MAVEN_SETTINGS_PREFIX = ArtifactRepositoryConstants.MAVEN_SETTINGS_PREFIX;
    protected static final String MAVEN_SETTINGS_DEFAULT = ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS_DEFAULT;

    protected static final String MAVEN_EMPTY_SETTINGS =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                    "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                    "</settings>\n";

    private final ControllerSettingAccessor controllerSettingAccessor;
    private final File localRepository;

    public MavenArtifactRepository(AutomationPackageManager manager, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor,  Configuration configuration, ControllerSettingAccessor controllerSettingAccessor) {
        super(Set.of(PARAM_GROUP_ID, PARAM_ARTIFACT_ID, PARAM_VERSION), manager, functionTypeRegistry, functionAccessor);
        localRepository = configuration.getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER));
        this.controllerSettingAccessor = controllerSettingAccessor;
    }

    private ControllerSetting getMavenSettings(Map<String, String> repositoryParameters) {
        String mavenSettingsId = MAVEN_SETTINGS_PREFIX + repositoryParameters.getOrDefault(PARAM_MAVEN_SETTINGS, MAVEN_SETTINGS_DEFAULT);
        ControllerSetting settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);

        if (settingsXml==null) {
            AbstractArtifactRepository.logger.warn("No settings found for \""+mavenSettingsId+"\", using empty settings instead.");
            controllerSettingAccessor.updateOrCreateSetting(mavenSettingsId,MAVEN_EMPTY_SETTINGS);
            settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);
        }
        return settingsXml;
    }

    @Override
    public File getArtifact(Map<String, String> repositoryParameters) {
        ControllerSetting settingsXml = getMavenSettings(repositoryParameters);
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(settingsXml.getValue(), localRepository);
            String artifactId = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTIFACT_ID);
            String version = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_VERSION);
            String groupId = AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_GROUP_ID);
            String classifier = repositoryParameters.get(PARAM_CLASSIFIER);
            String type = repositoryParameters.get(PARAM_TYPE);
            return mavenArtifactClient.getArtifact(new DefaultArtifact(groupId, artifactId, classifier, type == null || type.isEmpty() ? "jar" : type, version));
        } catch (SettingsBuildingException | ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String resolveArtifactName(Map<String, String> repositoryParameters) {
        return AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTIFACT_ID);
    }

}
