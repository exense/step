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
import step.automation.packages.*;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.ControllerServiceException;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanFilter;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.filters.PlanByExcludedNamesFilter;
import step.core.plans.filters.PlanByIncludedNamesFilter;
import step.core.plans.filters.PlanMultiFilter;
import step.core.repositories.*;
import step.functions.Function;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.ResourceManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.callPlan;

public abstract class AbstractArtifactRepository extends AbstractRepository {

	protected static final Logger logger = LoggerFactory.getLogger(MavenArtifactRepository.class);

	protected final PlanAccessor planAccessor;
	protected final ResourceManager resourceManager;
	protected final AutomationPackageReader automationPackageReader;

	public AbstractArtifactRepository(Set<String> canonicalRepositoryParameters, PlanAccessor planAccessor, ResourceManager resourceManager, AutomationPackageReader automationPackageReader) {
		super(canonicalRepositoryParameters);
		this.planAccessor = planAccessor;
		this.resourceManager = resourceManager;
		this.automationPackageReader = automationPackageReader;
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
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
		ParsedArtifact parsedArtifact = getAndParseArtifact(repositoryParameters, null);
		TestSetStatusOverview overview = new TestSetStatusOverview();
		List<TestRunStatus> runs = parsedArtifact.parsingResult.getPlans().stream()
				.map(plan -> new TestRunStatus(getPlanName(plan), getPlanName(plan), ReportNodeStatus.NORUN)).collect(Collectors.toList());
		overview.setRuns(runs);
		return overview;
	}

	protected ParsedArtifact getAndParseArtifact(Map<String, String> repositoryParameters, ObjectEnricher objectEnricher) {
		try {
			File artifact = getArtifact(repositoryParameters);
			PlanMultiFilter multiFilter = new PlanMultiFilter();
			if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS) != null) {
				multiFilter.add(new PlanByIncludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS))));
			}
			if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS) != null) {
				multiFilter.add(new PlanByExcludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS))));
			}
			PlansParsingResult parsingResult = parsePlans(artifact, multiFilter, objectEnricher);
			return new ParsedArtifact(artifact, parsingResult);
		} catch (AutomationPackageReadingException ex){
			// wrap into runtime exception
			throw new AutomationPackageManagerException("Unable to read automation package with the following repository params: " + repositoryParameters, ex);
		}
	}

	private List<String> parseList(String string) {
		return (string == null || string.isEmpty()) ? new ArrayList<>() : Arrays.stream(string.split(",")).collect(Collectors.toList());
	}

	protected abstract File getArtifact(Map<String, String> repositoryParameters);

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
		ImportResult result = new ImportResult();
		List<String> errors = new ArrayList<>();
		try {
			ParsedArtifact parsedArtifact = getAndParseArtifact(repositoryParameters, context.getObjectEnricher());
			Plan plan = buildAndStoreTestSetPlan(context, repositoryParameters, parsedArtifact);
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

	private Plan buildAndStoreTestSetPlan(ExecutionContext context, Map<String, String> repositoryParameters, ParsedArtifact parsedArtifact) {
		PlanBuilder planBuilder = PlanBuilder.create();
		int numberOfThreads = Integer.parseInt(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_THREAD_NUMBER, "0"));
		TestSet testSet = new TestSet(numberOfThreads);
		testSet.addAttribute(AbstractArtefact.NAME, parsedArtifact.artifact.getName());

		planBuilder.startBlock(testSet);
		parsedArtifact.parsingResult.getPlans().forEach(plan -> {
			String name = getPlanName(plan);

			wrapPlanInTestCase(plan, name);

			plan.setVisible(false);

			// TODO: can we link the non-java keywords (like jmeter keywords) in this way?
			if (plan.getFunctions() == null) {
				plan.setFunctions(new ArrayList<>());
			}
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

	protected PlansParsingResult parsePlans(File artifact, PlanFilter planFilter, ObjectEnricher enricher) throws AutomationPackageReadingException {
		AutomationPackageArchive automationPackageArchive = new AutomationPackageArchive(artifact);
		AutomationPackageContent content = automationPackageReader.readAutomationPackage(automationPackageArchive, false);

		// convert keywords from descriptor to functions
		AutomationPackageContext apContext = new AutomationPackageContext(resourceManager, automationPackageArchive, content, enricher, new HashMap<>());
        List<Function> functions = content.getKeywords().stream().map(keyword -> keyword.prepareKeyword(apContext)).collect(Collectors.toList());

		// filter plans if required
		return new PlansParsingResult(content.getPlans().stream().filter(p -> planFilter == null || planFilter.isSelected(p)).collect(Collectors.toList()), functions);
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

	protected static class ParsedArtifact {
		private final File artifact;
		private final PlansParsingResult parsingResult;

		public ParsedArtifact(File artifact, PlansParsingResult parsingResult) {

			this.artifact = artifact;
			this.parsingResult = parsingResult;
		}
	}

	protected static class PlansParsingResult {

		private final List<Plan> plans;
		private final List<Function> functions;

		public PlansParsingResult(List<Plan> plans, List<Function> functions) {
			this.plans = plans;
			this.functions = functions;
		}

		public List<Plan> getPlans() {
			return plans;
		}

		public List<Function> getFunctions() {
			return functions;
		}
	}
}
