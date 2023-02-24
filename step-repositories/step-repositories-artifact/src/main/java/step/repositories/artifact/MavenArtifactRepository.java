package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.plans.PlanAccessor;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class MavenArtifactRepository extends AbstractArtifactRepository {

    protected static final String PARAM_VERSION = "version";
    protected static final String PARAM_GROUP_ID = "groupId";
    protected static final String PARAM_CLASSIFIER = "classifier";

    protected static final String PARAM_LIB_VERSION = "libVersion";
    protected static final String PARAM_LIB_GROUP_ID = "libGroupId";
    protected static final String PARAM_LIB_CLASSIFIER = "libClassifier";

    protected static final String PARAM_MAVEN_SETTINGS = "mavenSettings";

    public static final String MAVEN_SETTINGS_PREFIX = "maven_settings_";
    protected static final String MAVEN_SETTINGS_DEFAULT = "default";
    protected static final String CONFIGURATION_MAVEN_FOLDER = "repository.artifact.maven.folder";
    protected static final String DEFAULT_MAVEN_FOLDER = "maven";
    protected static final String MAVEN_EMPTY_SETTINGS =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                    "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                    "</settings>\n";

    private final ControllerSettingAccessor controllerSettingAccessor;
    private final File localRepository;

    public MavenArtifactRepository(PlanAccessor planAccessor, ControllerSettingAccessor controllerSettingAccessor, Configuration configuration) {
        super(Set.of(PARAM_GROUP_ID, PARAM_ARTIFACT_ID, PARAM_VERSION), planAccessor);
        localRepository = configuration.getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER));
        this.controllerSettingAccessor = controllerSettingAccessor;
    }

    private ControllerSetting getMavenSettings(Map<String, String> repositoryParameters) {
        String mavenSettingsId = MAVEN_SETTINGS_PREFIX + repositoryParameters.getOrDefault(PARAM_MAVEN_SETTINGS, MAVEN_SETTINGS_DEFAULT);
        ControllerSetting settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);

        if (settingsXml==null) {
            logger.warn("No settings found for \""+mavenSettingsId+"\", using empty settings instead.");
            controllerSettingAccessor.updateOrCreateSetting(mavenSettingsId,MAVEN_EMPTY_SETTINGS);
            settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);
        }
        return settingsXml;
    }

    @Override
    protected File getLibraries(Map<String, String> repositoryParameters) {
        ControllerSetting settingsXml = getMavenSettings(repositoryParameters);
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(settingsXml.getValue(), localRepository);
            String artifactId = repositoryParameters.get(PARAM_LIB_ARTIFACT_ID);
            String version = repositoryParameters.get(PARAM_LIB_VERSION);
            String groupId = repositoryParameters.get(PARAM_LIB_GROUP_ID);
            String classifier = repositoryParameters.get(PARAM_LIB_CLASSIFIER);

            if (classifier!=null && artifactId==null) {
                artifactId = repositoryParameters.get(PARAM_ARTIFACT_ID);
            }

            if (artifactId!=null) {
                if (groupId==null) {
                    groupId = getMandatoryRepositoryParameter(repositoryParameters,PARAM_GROUP_ID);
                }
                if (version==null) {
                    version = getMandatoryRepositoryParameter(repositoryParameters,PARAM_VERSION);
                }
                return mavenArtifactClient.getArtifact(new DefaultArtifact(groupId, artifactId, classifier, "jar", version));
            } else {
                return null;
            }
        } catch (SettingsBuildingException | ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected File getArtifact(Map<String, String> repositoryParameters) {
        ControllerSetting settingsXml = getMavenSettings(repositoryParameters);
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(settingsXml.getValue(), localRepository);
            String artifactId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTIFACT_ID);
            String version = getMandatoryRepositoryParameter(repositoryParameters, PARAM_VERSION);
            String groupId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_GROUP_ID);
            String classifier = repositoryParameters.get(PARAM_CLASSIFIER);
            return mavenArtifactClient.getArtifact(new DefaultArtifact(groupId, artifactId, classifier, "jar", version));
        } catch (SettingsBuildingException | ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

}
