package step.repositories.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.ControllerServiceException;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.core.repositories.*;
import step.functions.Function;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.callPlan;

public abstract class AbstractArtifactRepository extends AbstractRepository {

	protected static final Logger logger = LoggerFactory.getLogger(MavenArtifactRepository.class);
	protected static final String PARAM_ARTIFACT_ID = "artifactId";
	protected static final String PARAM_LIB_ARTIFACT_ID = "libArtifactId";
	private static final String PARAM_THREAD_NUMBER = "threads";
	private static final String PARAM_INCLUDE_CLASSES = "includeClasses";
	private static final String PARAM_INCLUDE_ANNOTATIONS = "includeAnnotations";
	private static final String PARAM_EXCLUDE_CLASSES = "excludeClasses";
	private static final String PARAM_EXCLUDE_ANNOTATIONS = "excludeAnnotations";
	protected final PlanAccessor planAccessor;

	public AbstractArtifactRepository(Set<String> canonicalRepositoryParameters, PlanAccessor planAccessor) {
		super(canonicalRepositoryParameters);
		this.planAccessor = planAccessor;
	}

	protected static String getMandatoryRepositoryParameter(Map<String, String> repositoryParameters, String paramKey) {
		String value = repositoryParameters.get(paramKey);
		if (value == null) {
			throw new ControllerServiceException("Missing required parameter " + paramKey);
		}
		return value;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
		ArtefactInfo info = new ArtefactInfo();
		info.setName(resolveArtifactName(repositoryParameters));
		info.setType(TestSet.class.getSimpleName());
		return info;
	}

	protected String resolveArtifactName(Map<String, String> repositoryParameters) {
		return AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_ARTIFACT_ID);
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

	protected FileAndPlan getAndParseArtifact(Map<String, String> repositoryParameters) {
		File artifact = getArtifact(repositoryParameters);
		File libraries = getLibraries(repositoryParameters);

		String[] includedClasses = repositoryParameters.getOrDefault(PARAM_INCLUDE_CLASSES, ",").split(",");
		String[] includedAnnotations = repositoryParameters.getOrDefault(PARAM_INCLUDE_ANNOTATIONS, ",").split(",");
		String[] excludedClasses = repositoryParameters.getOrDefault(PARAM_EXCLUDE_CLASSES, ",").split(",");
		String[] excludedAnnotations = repositoryParameters.getOrDefault(PARAM_EXCLUDE_ANNOTATIONS, ",").split(",");

		List<Plan> plans = parsePlan(artifact,libraries,includedClasses,includedAnnotations,excludedClasses,excludedAnnotations);
		return new FileAndPlan(artifact, plans);
	}

	protected abstract File getLibraries(Map<String, String> repositoryParameters);

	protected abstract File getArtifact(Map<String, String> repositoryParameters);

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
