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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestCase;
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

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.callPlan;

public abstract class AbstractArtifactRepository extends AbstractRepository {

	protected static final Logger logger = LoggerFactory.getLogger(MavenArtifactRepository.class);

	private static final String PARAM_THREAD_NUMBER = "threads";
	private static final String PARAM_INCLUDE_CLASSES = "includeClasses";
	private static final String PARAM_INCLUDE_ANNOTATIONS = "includeAnnotations";
	private static final String PARAM_EXCLUDE_CLASSES = "excludeClasses";
	private static final String PARAM_EXCLUDE_ANNOTATIONS = "excludeAnnotations";
	protected final PlanAccessor planAccessor;
	protected final StepJarParser stepJarParser = new StepJarParser();

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

	protected abstract String resolveArtifactName(Map<String, String> repositoryParameters);

	@Override
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) {
		ParsedArtifact parsedArtifact = getAndParseArtifact(repositoryParameters);

		TestSetStatusOverview overview = new TestSetStatusOverview();
		List<TestRunStatus> runs = parsedArtifact.parsingResult.getPlans().stream()
				.map(plan -> new TestRunStatus(getPlanName(plan), getPlanName(plan), ReportNodeStatus.NORUN)).collect(Collectors.toList());
		overview.setRuns(runs);
		return overview;
	}

	protected ParsedArtifact getAndParseArtifact(Map<String, String> repositoryParameters) {
		File artifact = getArtifact(repositoryParameters);
		File libraries = getLibraries(repositoryParameters);

		String[] includedClasses = repositoryParameters.getOrDefault(PARAM_INCLUDE_CLASSES, ",").split(",");
		String[] includedAnnotations = repositoryParameters.getOrDefault(PARAM_INCLUDE_ANNOTATIONS, ",").split(",");
		String[] excludedClasses = repositoryParameters.getOrDefault(PARAM_EXCLUDE_CLASSES, ",").split(",");
		String[] excludedAnnotations = repositoryParameters.getOrDefault(PARAM_EXCLUDE_ANNOTATIONS, ",").split(",");

		StepJarParser.PlansParsingResult parsingResult = parsePlans(artifact,libraries,includedClasses,includedAnnotations,excludedClasses,excludedAnnotations);
		return new ParsedArtifact(artifact, parsingResult);
	}

	protected abstract File getLibraries(Map<String, String> repositoryParameters);

	protected abstract File getArtifact(Map<String, String> repositoryParameters);

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
		ImportResult result = new ImportResult();
		List<String> errors = new ArrayList<>();
		try {
			ParsedArtifact parsedArtifact = getAndParseArtifact(repositoryParameters);
			Plan plan = buildTestSetPlan(context, repositoryParameters, parsedArtifact);
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

	private Plan buildTestSetPlan(ExecutionContext context, Map<String, String> repositoryParameters, ParsedArtifact parsedArtifact) {
		PlanBuilder planBuilder = PlanBuilder.create();
		int numberOfThreads = Integer.parseInt(repositoryParameters.getOrDefault(PARAM_THREAD_NUMBER, "0"));
		TestSet testSet = new TestSet(numberOfThreads);
		testSet.addAttribute(AbstractArtefact.NAME, parsedArtifact.artifact.getName());

		planBuilder.startBlock(testSet);
		parsedArtifact.parsingResult.getPlans().forEach(plan -> {
			String name = getPlanName(plan);

			wrapPlanInTestCase(plan, name);

			plan.setVisible(false);

			plan.getFunctions().addAll(parsedArtifact.parsingResult.getFunctions());
			enrichPlan(context, plan);

			planAccessor.save(plan);
			planBuilder.add(callPlan(plan.getId().toString(), name));
		});
		planBuilder.endBlock();

		Plan plan = planBuilder.build();
		plan.setVisible(false);
		plan.setFunctions(parsedArtifact.parsingResult.getFunctions());
		enrichPlan(context, plan);
		return plan;
	}

	private String getPlanName(Plan plan) {
		return plan.getAttributes().get(AbstractOrganizableObject.NAME);
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

	}

	protected StepJarParser.PlansParsingResult parsePlans(File artifact, File libraries, String[] includedClasses, String[] includedAnnotations, String[] excludedClasses, String[] excludedAnnotations) {
		return stepJarParser.getPlansForJar(artifact, libraries, includedClasses, includedAnnotations, excludedClasses, excludedAnnotations);
	}

	protected void wrapPlanInTestCase(Plan plan, String testCaseName){
		AbstractArtefact root = plan.getRoot();
		if (!(root instanceof TestCase)) {
			// tricky solution - wrap all plans into TestCase to display all plans, launched while running automation package, in UI
			TestCase newRoot = new TestCase();
			newRoot.addAttribute(AbstractArtefact.NAME, testCaseName);
			newRoot.addChild(root);
			plan.setRoot(newRoot);
		}
	}

	private static class ParsedArtifact {
		private final File artifact;
		private final StepJarParser.PlansParsingResult parsingResult;

		public ParsedArtifact(File artifact, StepJarParser.PlansParsingResult parsingResult) {

			this.artifact = artifact;
			this.parsingResult = parsingResult;
		}
	}
}
