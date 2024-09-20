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
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.artefacts.AbstractArtefact;
import step.core.execution.ExecutionContext;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanFilter;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.filters.*;
import step.core.repositories.AbstractRepository;
import step.core.repositories.ImportResult;
import step.core.repositories.RepositoryObjectReference;
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
    public static final String PLAN_NAME = "planName";

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

    private static boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws IOException {
        PackageExecutionContext ctx = null;

        ImportResult result = new ImportResult();
        try {
            try {
                ctx = getOrRestorePackageExecutionContext(repositoryParameters, context.getObjectEnricher(), context.getObjectPredicate());
            } catch (AutomationPackageManagerException e) {
                result.setErrors(List.of(e.getMessage()));
                return result;
            }
            AutomationPackage automationPackage = ctx.getAutomationPackage();

            // PLAN_NAME but not PLAN_ID is used, because plan id is not persisted for isolated execution
            // (it is impossible to re-run the execution by plan id)
            String planName = repositoryParameters.get(PLAN_NAME);

            AutomationPackageManager apManager = ctx.getInMemoryManager();
            Plan plan;

            if (planName != null) {
                plan = apManager.getPackagePlans(automationPackage.getId())
                        .stream()
                        .filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(planName)).findFirst().orElse(null);
            } else {
                plan = wrapAllPlansFromApToTestSet(ctx, repositoryParameters);
            }

            if (plan == null) {
                // failed result
                result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan with name=" + planName));
                return result;
            }

            return importPlanForIsolatedExecution(context, result, plan, apManager, automationPackage);
        } catch (Exception e) {
            log.error("Error while importing / parsing artifact for execution " + context.getExecutionId(), e);
            List<String> errors = new ArrayList<>();
            errors.add("Error while importing / parsing artifact: " + e.getMessage());
            result.setSuccessful(false);
            result.setErrors(errors);
            return result;
        } finally {
            // if the context is created externally (shared for several plans), it should be managed (closed) in the calling code
            closePackageExecutionContext(ctx);
        }
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

    protected Stream<Plan> getFilteredPackagePlans(AutomationPackage ap, Map<String, String> repositoryParameters, AutomationPackageManager inMemoryManager) {
        PlanMultiFilter planFilter = getPlanFilter(repositoryParameters);
        return inMemoryManager.getPackagePlans(ap.getId()).stream().filter(p -> planFilter == null || planFilter.isSelected(p));
    }

    protected String getPlanName(Plan plan) {
        return plan.getAttributes().get(AbstractOrganizableObject.NAME);
    }

    public AutomationPackageFile getApFileForExecution(InputStream apInputStream, String inputStreamFileName, AutomationPackageExecutionParameters parameters, ObjectId contextId) {
        // for files provided by artifact repository we don't store the file as resource, but just load the file from this repository
        RepositoryObjectReference repositoryObject = parameters.getOriginalRepositoryObject();
        if (repositoryObject == null) {
            throw new AutomationPackageManagerException("Unable to resolve AP file. Repository object is undefined");
        }
        File artifact = getArtifact(parameters.getOriginalRepositoryObject().getRepositoryParameters());
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
            // Here we resolve the original AP file used for previous isolated execution and re-use it to create the execution context
            AutomationPackageFile apFile = restoreApFile(contextId, repositoryParameters);
            return createPackageExecutionContext(enricher, predicate, contextId, apFile, false);
        }
        return current;
    }

    protected AutomationPackageFile restoreApFile(String contextId, Map<String, String> repositoryParameters) {
        File artifact = getArtifact(repositoryParameters);
        if (artifact == null) {
            throw new AutomationPackageManagerException("Unable to resolve the requested Automation Package file in artifact repository " + this.getClass().getSimpleName() + " with parameters " + repositoryParameters);
        }
        return new AutomationPackageFile(artifact, null);
    }

    public PackageExecutionContext createPackageExecutionContext(ObjectEnricher enricher, ObjectPredicate predicate, String contextId, AutomationPackageFile apFile, boolean shared) {
        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                new ObjectId(contextId), functionTypeRegistry,
                functionAccessor
        );

        // create single automation package in isolated manager
        try (FileInputStream fis = new FileInputStream(apFile.getFile())) {
            inMemoryPackageManager.createAutomationPackage(fis, apFile.getFile().getName(), enricher, predicate);
        } catch (IOException e) {
            throw new AutomationPackageManagerException("Cannot read the AP file: " + apFile.getFile().getName());
        }

        PackageExecutionContext res = new PackageExecutionContext(contextId, inMemoryPackageManager, shared);
        if (shared) {
            sharedPackageExecutionContexts.put(contextId, res);
        }
        return res;
    }

    public void setApNameForResource(Resource resource, String apName){
        resource.addCustomField(AP_NAME_CUSTOM_FIELD, apName);
    }

    protected ImportResult importPlanForIsolatedExecution(ExecutionContext context, ImportResult result, Plan plan, AutomationPackageManager apManager, AutomationPackage automationPackage) {
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

    protected void closePackageExecutionContext(PackageExecutionContext ctx) throws IOException {
        if (ctx != null && !ctx.isExternallyCreatedContext()) {
            ctx.close();
        }
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

    public class PackageExecutionContext implements Closeable {
        private final String contextId;
        private final AutomationPackageManager inMemoryManager;
        private final boolean externallyCreatedContext;

        public PackageExecutionContext(String contextId, AutomationPackageManager inMemoryManager, boolean externallyCreatedContext) {
            this.contextId = contextId;
            this.inMemoryManager = inMemoryManager;
            this.externallyCreatedContext = externallyCreatedContext;
        }

        public AutomationPackageManager getInMemoryManager() {
            return inMemoryManager;
        }

        public AutomationPackage getAutomationPackage() {
            return getInMemoryManager().getAllAutomationPackages(null).findFirst().orElse(null);
        }

        public boolean isExternallyCreatedContext() {
            return externallyCreatedContext;
        }

        @Override
        public void close() throws IOException {

            // only after isolated execution is finished we can clean up temporary created resources
            try {
                // remove the context from isolated automation package repository
                log.info("Cleanup isolated execution context");

                IsolatedAutomationPackageRepository.PackageExecutionContext automationPackageManager = sharedPackageExecutionContexts.get(contextId);
                if (automationPackageManager != null) {
                    automationPackageManager.getInMemoryManager().cleanup();
                }
            } finally {
                sharedPackageExecutionContexts.remove(contextId);
            }
        }
    }
}
