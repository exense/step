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
package step.automation.packages.execution;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.automation.packages.AutomationPackageFileSource;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanFilter;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.filters.*;
import step.core.repositories.*;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.LayeredResourceManager;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.planbuilder.BaseArtefacts.callPlan;

public abstract class RepositoryWithAutomationPackageSupport extends AbstractRepository {

    public static final Logger log = LoggerFactory.getLogger(RepositoryWithAutomationPackageSupport.class);

    public static final String AP_NAME = "apName";
    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";
    public static final String AP_NAME_CUSTOM_FIELD = "apName";

    public static final String CONFIGURATION_MAVEN_FOLDER = "repository.artifact.maven.folder";
    public static final String DEFAULT_MAVEN_FOLDER = "maven";

    // context id -> automation package manager (cache)
    protected final ConcurrentHashMap<String, PackageExecutionContext> sharedPackageExecutionContexts = new ConcurrentHashMap<>();
    protected final AutomationPackageManager manager;
    protected final FunctionTypeRegistry functionTypeRegistry;
    protected final FunctionAccessor functionAccessor;

    public RepositoryWithAutomationPackageSupport(Set<String> canonicalRepositoryParameters, AutomationPackageManager manager, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor) {
        super(canonicalRepositoryParameters);
        this.manager = manager;
        this.functionTypeRegistry = functionTypeRegistry;
        this.functionAccessor = functionAccessor;
    }

    protected boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws Exception {
        PackageExecutionContext ctx = null;
        try {
            // TODO: pass library file
            File artifact = getArtifact(repositoryParameters, objectPredicate);
            ctx = createIsolatedPackageExecutionContext(null, objectPredicate, new ObjectId().toString(), new AutomationPackageFile(artifact, null), false, null);
            TestSetStatusOverview overview = new TestSetStatusOverview();
            List<TestRunStatus> runs = getFilteredPackagePlans(ctx.getAutomationPackage(), repositoryParameters, ctx.getAutomationPackageManager())
                    .map(plan -> new TestRunStatus(getPlanName(plan), getPlanName(plan), ReportNodeStatus.NORUN)).collect(Collectors.toList());
            overview.setRuns(runs);
            return overview;
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws IOException {
        PackageExecutionContext ctx = null;

        ImportResult result = new ImportResult();
        try {
            try {
                ctx = getOrRestorePackageExecutionContext(repositoryParameters, context.getObjectEnricher(), context.getObjectPredicate());
                //If context is shared across multiple executions, it was created externally and will be closed by the creator,
                // otherwise it should be closed once the executions ends from the execution context
                if (!ctx.isShared()) {
                    context.put(PackageExecutionContext.class, ctx);
                }
            } catch (AutomationPackageManagerException e) {
                result.setErrors(List.of(e.getMessage()));
                return result;
            }
            AutomationPackage automationPackage = ctx.getAutomationPackage();
            AutomationPackageManager apManager = ctx.getAutomationPackageManager();
            Plan plan;

            boolean wrapped = false;
            if (!isWrapPlansIntoTestSet(repositoryParameters)) {
                // if we don't wrap into test set, we should have one and only filtered plan
                List<Plan> filteredPlans = getFilteredPackagePlans(automationPackage, repositoryParameters, ctx.getAutomationPackageManager()).collect(Collectors.toList());
                if (filteredPlans.isEmpty()) {
                    result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no applicable plan to execute"));
                    return result;
                }
                if (filteredPlans.size() > 1) {
                    result.setErrors(List.of("Automation package " +
                            automationPackage.getAttribute(AbstractOrganizableObject.NAME) +
                            " has ambiguous plan for execution: " +
                            filteredPlans.stream().map(p -> p.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.toList()))
                    );
                    return result;
                }

                plan = filteredPlans.get(0);
            } else {
                plan = wrapAllPlansFromApToTestSet(ctx, repositoryParameters);
                wrapped = true;
            }

            if (plan == null) {
                // failed result
                result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan for execution"));
                return result;
            }

            return importPlanForExecutionWithinAp(context, result, plan, apManager, automationPackage, wrapped);
        } catch (Exception e) {
            log.error("Error while importing / parsing artifact for execution " + context.getExecutionId(), e);
            List<String> errors = new ArrayList<>();
            errors.add("Error while importing / parsing artifact: " + e.getMessage());
            result.setSuccessful(false);
            result.setErrors(errors);
            return result;
        }
    }

    @Override
    public void postExecution(ExecutionContext context, RepositoryObjectReference repositoryObjectReference) throws Exception {
        super.postExecution(context, repositoryObjectReference);
        RepositoryWithAutomationPackageSupport.PackageExecutionContext packageExecutionContext = context.get(RepositoryWithAutomationPackageSupport.PackageExecutionContext.class);
        if (packageExecutionContext != null) {
            try {
                packageExecutionContext.close();
            } catch (IOException e) {
                log.error("Unable to clean up the automation package context for execution {}", context.getExecutionId(), e);
            }
        }
    }

    protected boolean isWrapPlansIntoTestSet(Map<String, String> repositoryParameters) {
        // by default, we wrap plans into test set
        return true;
    }

    private Plan wrapAllPlansFromApToTestSet(PackageExecutionContext ctx, Map<String, String> repositoryParameters) {
        PlanBuilder planBuilder = PlanBuilder.create();
        int numberOfThreads = Integer.parseInt(repositoryParameters.getOrDefault(ArtifactRepositoryConstants.PARAM_THREAD_NUMBER, "0"));
        TestSet testSet = new TestSet(numberOfThreads);
        AutomationPackage ap = ctx.getAutomationPackage();
        testSet.addAttribute(AbstractArtefact.NAME, ap.getAttribute(AbstractOrganizableObject.NAME));

        planBuilder.startBlock(testSet);
        getFilteredPackagePlans(ap, repositoryParameters, ctx.getAutomationPackageManager()).forEach(plan -> {
            String name = getPlanName(plan);
            wrapPlanInTestCase(plan, name);
            planBuilder.add(callPlan(plan.getId().toString(), name));
        });
        planBuilder.endBlock();

        return planBuilder.build();
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

    protected PlanMultiFilter getPlanFilter(Map<String, String> repositoryParameters) {
        List<PlanFilter> multiFilter = new ArrayList<>();
        if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS) != null) {
            multiFilter.add(new PlanByIncludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_PLANS))));
        }
        if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS) != null) {
            multiFilter.add(new PlanByExcludedNamesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_PLANS))));
        }
        if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_CATEGORIES) != null) {
            multiFilter.add(new PlanByIncludedCategoriesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_INCLUDE_CATEGORIES))));
        }
        if (repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_CATEGORIES) != null) {
            multiFilter.add(new PlanByExcludedCategoriesFilter(parseList(repositoryParameters.get(ArtifactRepositoryConstants.PARAM_EXCLUDE_CATEGORIES))));
        }
        return new PlanMultiFilter(multiFilter);
    }

    private List<String> parseList(String string) {
        return (string == null || string.isBlank()) ? new ArrayList<>() : Arrays.stream(string.split(",")).collect(Collectors.toList());
    }

    protected Stream<Plan> getFilteredPackagePlans(AutomationPackage ap, Map<String, String> repositoryParameters, AutomationPackageManager apManager) {
        PlanMultiFilter planFilter = getPlanFilter(repositoryParameters);
        return apManager.getPackagePlans(ap.getId()).stream().filter(p -> planFilter == null || planFilter.isSelected(p));
    }

    protected String getPlanName(Plan plan) {
        return plan.getAttributes().get(AbstractOrganizableObject.NAME);
    }

    public AutomationPackageFile getApFileForExecution(InputStream apInputStream, String inputStreamFileName, IsolatedAutomationPackageExecutionParameters parameters, ObjectId contextId, ObjectPredicate objectPredicate, String actorUser) {
        // for files provided by artifact repository we don't store the file as resource, but just load the file from this repository
        RepositoryObjectReference repositoryObject = parameters.getOriginalRepositoryObject();
        if (repositoryObject == null) {
            throw new AutomationPackageManagerException("Unable to resolve AP file. Repository object is undefined");
        }
        File artifact = getArtifact(parameters.getOriginalRepositoryObject().getRepositoryParameters(), objectPredicate);
        return new AutomationPackageFile(artifact, null);
    }

    protected PackageExecutionContext getOrRestorePackageExecutionContext(Map<String, String> repositoryParameters, ObjectEnricher enricher, ObjectPredicate predicate) {
        String contextId = repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID);

        // Execution context can be created in-advance and shared between several plans
        PackageExecutionContext current = contextId == null ? null : sharedPackageExecutionContexts.get(contextId);
        if (current == null) {
            if (contextId == null) {
                contextId = new ObjectId().toString();
            }
            // TODO: pass keyword lib
            // Here we resolve the original AP file used for previous isolated execution and re-use it to create the execution context
            AutomationPackageFile apFile = restoreApFile(contextId, repositoryParameters, predicate);
            return createIsolatedPackageExecutionContext(enricher, predicate, contextId, apFile, false, null);
        }
        return current;
    }

    protected AutomationPackageFile restoreApFile(String contextId, Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) {
        File artifact = getArtifact(repositoryParameters, objectPredicate);
        if (artifact == null) {
            throw new AutomationPackageManagerException("Unable to resolve the requested Automation Package file in artifact repository " + this.getClass().getSimpleName() + " with parameters " + repositoryParameters);
        }
        return new AutomationPackageFile(artifact, null);
    }

    public PackageExecutionContext createIsolatedPackageExecutionContext(ObjectEnricher enricher, ObjectPredicate predicate, String contextId, AutomationPackageFile apFile, boolean shared,
                                                                         AutomationPackageFileSource keywordLibrarySource) {
        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                new ObjectId(contextId), functionTypeRegistry,
                functionAccessor
        );

        // create single automation package in isolated manager
        try (FileInputStream fis = new FileInputStream(apFile.getFile())) {
            // the apVersion is null (we always use the actual version), because we only create the isolated in-memory AP here
            // TODO: actorUser?
            inMemoryPackageManager.createAutomationPackage(AutomationPackageFileSource.withInputStream(fis, apFile.getFile().getName()), null, null, keywordLibrarySource, null, enricher, predicate);
        } catch (IOException e) {
            throw new AutomationPackageManagerException("Cannot read the AP file: " + apFile.getFile().getName());
        }

        PackageExecutionContext res = new IsolatedPackageExecutionContext(contextId, inMemoryPackageManager, shared);
        if (shared) {
            sharedPackageExecutionContexts.put(contextId, res);
        }
        return res;
    }

    public void setApNameForResource(Resource resource, String apName){
        resource.addCustomField(AP_NAME_CUSTOM_FIELD, apName);
    }

    protected ImportResult importPlanForExecutionWithinAp(ExecutionContext context, ImportResult result,
                                                          Plan plan, AutomationPackageManager apManager,
                                                          AutomationPackage automationPackage,
                                                          boolean fakeWrappedPlan) {
        result.setPlanId(plan.getId().toString());
        enrichPlan(context, plan);

        // the plan accessor in context should be layered with 'inMemory' accessor on the top to temporarily store
        // all plans from AP (in code below)
        PlanAccessor planAccessor = context.getPlanAccessor();
        if (!isLayeredAccessor(planAccessor)) {
            result.setErrors(List.of(planAccessor.getClass() + " is not layered"));
            return result;
        }

        planAccessor.save(plan);

        // save ALL plans from AP to the execution context to support the 'callPlan' artefact
        // (if some plan from the AP is call from 'callPlan', it should be saved in execution context)
        for (Plan packagePlan : apManager.getPackagePlans(automationPackage.getId())) {
            enrichPlan(context, packagePlan);
            planAccessor.save(packagePlan);
        }

        // populate function accessor for execution context with all functions loaded from AP
        FunctionAccessor functionAccessor = context.get(FunctionAccessor.class);
        List<Function> functionsForSave = new ArrayList<>();
        if (plan.getFunctions() != null) {
            plan.getFunctions().iterator().forEachRemaining(functionsForSave::add);
        }
        functionsForSave.addAll(apManager.getPackageFunctions(automationPackage.getId()));
        functionAccessor.save(functionsForSave);

        ResourceManager contextResourceManager = context.getResourceManager();
        if (!(contextResourceManager instanceof LayeredResourceManager)) {
            result.setErrors(List.of(contextResourceManager.getClass() + " is not layered"));
            return result;
        }

        // import all resources from automation package to execution context by adding the layer to contextResourceManager
        // resource manager used in isolated package manager is non-permanent
        ((LayeredResourceManager) contextResourceManager).pushManager(apManager.getResourceManager(), false);

        // call some hooks on import
        apManager.runExtensionsBeforeIsolatedExecution(automationPackage, context, apManager.getExtensions(), result);

        result.setSuccessful(true);

        return result;
    }

    public static class AutomationPackageFile {
        private final File file;
        private final Resource resource;

        public AutomationPackageFile(File file, Resource localResource) {
            this.file = file;
            this.resource = localResource;
        }

        public File getFile() {
            return file;
        }

        public Resource getResource() {
            return resource;
        }
    }

    public interface PackageExecutionContext extends Closeable {
        AutomationPackageManager getAutomationPackageManager();

        AutomationPackage getAutomationPackage();

        boolean isShared();
    }

    public static class LocalPackageExecutionContext implements PackageExecutionContext {

        private final String contextId;
        private final AutomationPackageManager automationPackageManager;
        private final AutomationPackage automationPackage;

        public LocalPackageExecutionContext(String contextId, AutomationPackageManager automationPackageManager, AutomationPackage automationPackage) {
            this.contextId = contextId;
            this.automationPackageManager = automationPackageManager;
            this.automationPackage = automationPackage;
        }

        @Override
        public AutomationPackageManager getAutomationPackageManager() {
            return automationPackageManager;
        }

        @Override
        public AutomationPackage getAutomationPackage() {
            return automationPackage;
        }

        @Override
        public boolean isShared() {
            return false;
        }

        @Override
        public void close() throws IOException {

        }
    }

    public class IsolatedPackageExecutionContext implements PackageExecutionContext {
        private final String contextId;
        private final AutomationPackageManager inMemoryManager;
        private final boolean shared;

        public IsolatedPackageExecutionContext(String contextId, AutomationPackageManager inMemoryManager, boolean shared) {
            this.contextId = contextId;
            this.inMemoryManager = inMemoryManager;
            this.shared = shared;
        }

        @Override
        public AutomationPackageManager getAutomationPackageManager() {
            return inMemoryManager;
        }

        @Override
        public AutomationPackage getAutomationPackage() {
            return inMemoryManager.getAllAutomationPackages(null).findFirst().orElse(null);
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public void close() throws IOException {
            // cleanup the associated automation package manager and remove this context from the shared map in case of shared context
            log.info("Cleanup isolated execution context");
            //In case the Package execution context is shared (i.e. when triggering isolated executions from CLI), we close the shared context
            //and remove it from the shared map
            if (shared) {
                IsolatedAutomationPackageRepository.PackageExecutionContext automationPackageManager = sharedPackageExecutionContexts.remove(contextId);
                if (automationPackageManager != null) {
                    automationPackageManager.getAutomationPackageManager().cleanup();
                }
            //Otherwise directly clean the automation package stored in this context
            } else {
                inMemoryManager.cleanup();
            }
        }
    }
}
