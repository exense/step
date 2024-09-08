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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.automation.packages.*;
import step.automation.packages.execution.RepositoryWithAutomationPackageSupport;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.ControllerServiceException;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.filters.PlanByExcludedNamesFilter;
import step.core.plans.filters.PlanByIncludedNamesFilter;
import step.core.plans.filters.PlanMultiFilter;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.TestRunStatus;
import step.core.repositories.TestSetStatusOverview;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.planbuilder.BaseArtefacts.callPlan;

public abstract class AbstractArtifactRepository extends RepositoryWithAutomationPackageSupport {

	protected static final Logger logger = LoggerFactory.getLogger(MavenArtifactRepository.class);

	public AbstractArtifactRepository(Set<String> canonicalRepositoryParameters, AutomationPackageManager manager, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor) {
		super(canonicalRepositoryParameters, manager, functionTypeRegistry, functionAccessor);
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
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws IOException {
		PackageExecutionContext ctx = null;
		try {
			ctx = createPackageExecutionContext(null, objectPredicate, new ObjectId().toString(), repositoryParameters);
			TestSetStatusOverview overview = new TestSetStatusOverview();
			List<TestRunStatus> runs = getFilteredPackagePlans(ctx.getAutomationPackage(), repositoryParameters, ctx.getInMemoryManager())
					.map(plan -> new TestRunStatus(getPlanName(plan), getPlanName(plan), ReportNodeStatus.NORUN)).collect(Collectors.toList());
			overview.setRuns(runs);
			return overview;
		} finally {
			closePackageExecutionContext(ctx);
		}
	}

	protected PlanMultiFilter getPlanFilter(Map<String, String> repositoryParameters) {
		PlanMultiFilter multiFilter = new PlanMultiFilter();
		if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS) != null) {
			multiFilter.add(new PlanByIncludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS))));
		}
		if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS) != null) {
			multiFilter.add(new PlanByExcludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS))));
		}
		return multiFilter;
	}

	private List<String> parseList(String string) {
		return (string == null || string.isEmpty()) ? new ArrayList<>() : Arrays.stream(string.split(",")).collect(Collectors.toList());
	}

	public abstract File getArtifact(Map<String, String> repositoryParameters);

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws IOException {
		ImportResult result = new ImportResult();
		List<String> errors = new ArrayList<>();
		PackageExecutionContext ctx = null;
		try {
			ctx = createPackageExecutionContext(context.getObjectEnricher(), context.getObjectPredicate(), new ObjectId().toString(), repositoryParameters);
			Plan testSet = wrapAllPlansFromApToTestSet(ctx, repositoryParameters);
			return importPlanForIsolatedExecution(context, result, testSet, ctx.getInMemoryManager(), ctx.getAutomationPackage());
		} catch (Exception e) {
			logger.error("Error while importing / parsing artifact for execution " + context.getExecutionId(), e);
			errors.add("Error while importing / parsing artifact: " + e.getMessage());
			result.setSuccessful(false);
			result.setErrors(errors);
			return result;
		} finally {
			closePackageExecutionContext(ctx);
		}
	}

	private PackageExecutionContext createPackageExecutionContext(ObjectEnricher enricher, ObjectPredicate predicate, String contextId, Map<String, String> repositoryParameters) {
		File artifact = getArtifact(repositoryParameters);
		return super.createPackageExecutionContext(enricher, predicate, contextId, new AutomationPackageFile(artifact, null));
	}

	private Plan wrapAllPlansFromApToTestSet(PackageExecutionContext ctx, Map<String, String> repositoryParameters) {
		PlanBuilder planBuilder = PlanBuilder.create();
		int numberOfThreads = Integer.parseInt(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_THREAD_NUMBER, "0"));
		TestSet testSet = new TestSet(numberOfThreads);
		AutomationPackage ap = ctx.getAutomationPackage();
		testSet.addAttribute(AbstractArtefact.NAME, ap.getAttribute(AbstractOrganizableObject.NAME));

		planBuilder.startBlock(testSet);
		getFilteredPackagePlans(ap, repositoryParameters, ctx.getInMemoryManager()).forEach(plan -> {
			String name = getPlanName(plan);
			wrapPlanInTestCase(plan, name);
			planBuilder.add(callPlan(plan.getId().toString(), name));
		});
		planBuilder.endBlock();

		return planBuilder.build();
	}

	private Stream<Plan> getFilteredPackagePlans(AutomationPackage ap, Map<String, String> repositoryParameters, AutomationPackageManager inMemoryManager) {
		PlanMultiFilter planFilter = getPlanFilter(repositoryParameters);
		return inMemoryManager.getPackagePlans(ap.getId()).stream().filter(p -> planFilter == null || planFilter.isSelected(p));
	}

	private String getPlanName(Plan plan) {
		return plan.getAttributes().get(AbstractOrganizableObject.NAME);
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

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

		public File getArtifact() {
			return artifact;
		}

		public PlansParsingResult getParsingResult() {
			return parsingResult;
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
