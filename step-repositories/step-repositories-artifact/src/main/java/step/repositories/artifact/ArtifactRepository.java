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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.callPlan;

public class ArtifactRepository extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepository.class);

    protected static final String PARAM_ARTEFACT_ID = "artefactId";
    protected static final String PARAM_VERSION = "version";
    protected static final String PARAM_GROUP_ID = "groupId";
    protected static final String PARAM_CLASSIFIER = "classifier";

    private static final String PARAM_THREAD_NUMBER = "NbThreads";
    public static final String MAVEN_SETTINGS_XML = "maven_settings_xml";

    private final PlanAccessor planAccessor;
    private final ControllerSettingAccessor controllerSettingAccessor;
    private final File localRepository;

    public ArtifactRepository(PlanAccessor planAccessor, ControllerSettingAccessor controllerSettingAccessor, Configuration configuration) {
        super(Set.of(PARAM_GROUP_ID, PARAM_ARTEFACT_ID, PARAM_VERSION));
        localRepository = configuration.getPropertyAsFile("repository.artifact.maven.folder", new File("maven"));
        this.planAccessor = planAccessor;
        this.controllerSettingAccessor = controllerSettingAccessor;
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
        ArtefactInfo info = new ArtefactInfo();
        info.setName(getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTEFACT_ID));
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
        ControllerSetting settingsXml = controllerSettingAccessor.getSettingByKey(MAVEN_SETTINGS_XML);
        File artifact = getArtifact(repositoryParameters, settingsXml);
        List<Plan> plans = parsePlan(artifact);
        return new FileAndPlan(artifact, plans);
    }

    private File getArtifact(Map<String, String> repositoryParameters, ControllerSetting settingsXml) {
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(settingsXml.getValue(), localRepository);
            String groupId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_GROUP_ID);
            String artifactId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTEFACT_ID);
            String version = getMandatoryRepositoryParameter(repositoryParameters, PARAM_VERSION);
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

            PlanBuilder planBuilder = PlanBuilder.create();
            TestSet testSet = new TestSet(
                    Integer.parseInt(repositoryParameters.getOrDefault(PARAM_THREAD_NUMBER, "0")));
            testSet.getAttributes().put(AbstractArtefact.NAME, fileAndPlan.artifact.getName());

            planBuilder.startBlock(testSet);

            List<Function> functions = new ArrayList<>();
            fileAndPlan.plans.forEach(plan -> {
                String name = getPlanName(plan);
                plan.setVisible(false);
                functions.addAll(plan.getFunctions());
                planAccessor.save(plan);
                planBuilder.add(callPlan(plan.getId().toString(), name));
            });

            planBuilder.endBlock();

            Plan plan = planBuilder.build();
            plan.setVisible(false);
            plan.setFunctions(functions);
            enrichPlan(context, plan);
            planAccessor.save(plan);
            result.setPlanId(plan.getId().toString());

        } catch (Exception e) {
            logger.error("Error while importing artefact for " + context.getExecutionId(), e);
            errors.add("General error when trying to create the test set. Exception: " + e.getMessage());
        }

        result.setSuccessful(errors.isEmpty());
        result.setErrors(errors);

        return result;
    }

    private String getPlanName(Plan plan) {
        return plan.getAttributes().get(AbstractOrganizableObject.NAME);
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

    }

    private List<Plan> parsePlan(File file) {
        return new StepJarParser().getPlansForJar(file);
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
