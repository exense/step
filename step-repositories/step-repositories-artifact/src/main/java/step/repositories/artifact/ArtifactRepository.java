package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.deployment.ControllerServiceException;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.core.repositories.*;
import step.functions.Function;
import step.repositories.ArtifactRepositoryConstants;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.callPlan;

public class ArtifactRepository extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepository.class);

    protected static final String PARAM_ARTIFACT_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID;
    protected static final String PARAM_VERSION = ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION;
    protected static final String PARAM_GROUP_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID;
    protected static final String PARAM_CLASSIFIER = ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER;

    protected static final String PARAM_LIB_ARTIFACT_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_LIB_ARTIFACT_ID;
    protected static final String PARAM_LIB_VERSION = ArtifactRepositoryConstants.ARTIFACT_PARAM_LIB_VERSION;
    protected static final String PARAM_LIB_GROUP_ID = ArtifactRepositoryConstants.ARTIFACT_PARAM_LIB_GROUP_ID;
    protected static final String PARAM_LIB_CLASSIFIER = ArtifactRepositoryConstants.ARTIFACT_PARAM_LIB_CLASSIFIER;

    protected static final String PARAM_MAVEN_SETTINGS = "mavenSettings";
    private static final String PARAM_THREAD_NUMBER = "threads";
    private static final String PARAM_INCLUDE_CLASSES = "includeClasses";
    private static final String PARAM_INCLUDE_ANNOTATIONS = "includeAnnotations";
    private static final String PARAM_EXCLUDE_CLASSES = "excludeClasses";
    private static final String PARAM_EXCLUDE_ANNOTATIONS = "excludeAnnotations";

    public static final String MAVEN_SETTINGS_PREFIX = "maven_settings_";
    protected static final String MAVEN_SETTINGS_DEFAULT = "default";
    protected static final String CONFIGURATION_MAVEN_FOLDER = "repository.artifact.maven.folder";
    protected static final String DEFAULT_MAVEN_FOLDER = "maven";
    protected static final String MAVEN_EMPTY_SETTINGS =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                    "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                    "</settings>\n";

    private final PlanAccessor planAccessor;
    private final ControllerSettingAccessor controllerSettingAccessor;
    private final File localRepository;

    public ArtifactRepository(PlanAccessor planAccessor, ControllerSettingAccessor controllerSettingAccessor, Configuration configuration) {
        super(Set.of(PARAM_GROUP_ID, PARAM_ARTIFACT_ID, PARAM_VERSION));
        localRepository = configuration.getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER));
        this.planAccessor = planAccessor;
        this.controllerSettingAccessor = controllerSettingAccessor;
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
        ArtefactInfo info = new ArtefactInfo();
        info.setName(getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTIFACT_ID));
        info.setType(TestSet.class.getSimpleName());
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) {
        FileAndPlan fileAndPlan = getAndParseArtifact(repositoryParameters);

        TestSetStatusOverview overview = new TestSetStatusOverview();
        List<TestRunStatus> runs = fileAndPlan.plans.stream()
                .map(plan -> new TestRunStatus(getPlanName(plan), getPlanName(plan), ReportNodeStatus.NORUN)).collect(Collectors.toList());
        overview.setRuns(runs);
        return overview;
    }

    private FileAndPlan getAndParseArtifact(Map<String, String> repositoryParameters) {
        String mavenSettingsId = MAVEN_SETTINGS_PREFIX + repositoryParameters.getOrDefault(PARAM_MAVEN_SETTINGS, MAVEN_SETTINGS_DEFAULT);
        ControllerSetting settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);

        if (settingsXml==null) {
            logger.warn("No settings found for \""+mavenSettingsId+"\", using empty settings instead.");
            controllerSettingAccessor.updateOrCreateSetting(mavenSettingsId,MAVEN_EMPTY_SETTINGS);
            settingsXml = controllerSettingAccessor.getSettingByKey(mavenSettingsId);
        }

        File artifact = getArtifact(repositoryParameters, settingsXml);

        File libraries = getLibraries(repositoryParameters, settingsXml);

        String[] includedClasses = repositoryParameters.getOrDefault(PARAM_INCLUDE_CLASSES, ",").split(",");
        String[] includedAnnotations = repositoryParameters.getOrDefault(PARAM_INCLUDE_ANNOTATIONS, ",").split(",");
        String[] excludedClasses = repositoryParameters.getOrDefault(PARAM_EXCLUDE_CLASSES, ",").split(",");
        String[] excludedAnnotations = repositoryParameters.getOrDefault(PARAM_EXCLUDE_ANNOTATIONS, ",").split(",");

        List<Plan> plans = parsePlan(artifact,libraries,includedClasses,includedAnnotations,excludedClasses,excludedAnnotations);
        return new FileAndPlan(artifact, plans);
    }

    private File getLibraries(Map<String, String> repositoryParameters, ControllerSetting settingsXml) {
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

    private File getArtifact(Map<String, String> repositoryParameters, ControllerSetting settingsXml) {
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

    private static String getMandatoryRepositoryParameter(Map<String, String> repositoryParameters, String paramKey) {
        String value = repositoryParameters.get(paramKey);
        if (value == null) {
            throw new ControllerServiceException("Missing required parameter " + paramKey);
        }
        return value;
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();
        try {
            FileAndPlan fileAndPlan = getAndParseArtifact(repositoryParameters);
            Plan plan = buildTestSetPlan(context, repositoryParameters, fileAndPlan);
            planAccessor.save(plan);
            result.setPlanId(plan.getId().toString());
        } catch (Exception e) {
            logger.error("Error while importing / parsing artifact for execution " + context.getExecutionId(), e);
            errors.add("Error while importing / parsing artifact: " + e.getMessage());
        }
        result.setSuccessful(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    private Plan buildTestSetPlan(ExecutionContext context, Map<String, String> repositoryParameters, FileAndPlan fileAndPlan) {
        PlanBuilder planBuilder = PlanBuilder.create();
        int numberOfThreads = Integer.parseInt(repositoryParameters.getOrDefault(PARAM_THREAD_NUMBER, "0"));
        TestSet testSet = new TestSet(numberOfThreads);
        testSet.addAttribute(AbstractArtefact.NAME, fileAndPlan.artifact.getName());

        planBuilder.startBlock(testSet);
        List<Function> functions = new ArrayList<>();
        fileAndPlan.plans.forEach(plan -> {
            String name = getPlanName(plan);

            plan.setVisible(false);

            Collection<Function> testCaseFunctions = plan.getFunctions();

            plan.setFunctions(testCaseFunctions);
            functions.addAll(testCaseFunctions);

            enrichPlan(context, plan);
            
            planAccessor.save(plan);
            planBuilder.add(callPlan(plan.getId().toString(), name));
        });
        planBuilder.endBlock();

        Plan plan = planBuilder.build();
        plan.setVisible(false);
        plan.setFunctions(functions);
        enrichPlan(context, plan);
        return plan;
    }

    private String getPlanName(Plan plan) {
        return plan.getAttributes().get(AbstractOrganizableObject.NAME);
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

    }

    private List<Plan> parsePlan(File artifact, File libraries, String[] includedClasses, String[] includedAnnotations, String[] excludedClasses, String[] excludedAnnotations) {
        return new StepJarParser().getPlansForJar(artifact,libraries,includedClasses,includedAnnotations,excludedClasses,excludedAnnotations);
    }

    private static class FileAndPlan {
        private final File artifact;
        private final List<Plan> plans;

        public FileAndPlan(File artifact, List<Plan> plans) {

            this.artifact = artifact;
            this.plans = plans;
        }
    }
}
