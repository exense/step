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
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.AbstractRepository;
import step.core.repositories.ImportResult;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.LayeredResourceManager;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RepositoryWithAutomationPackageSupport extends AbstractRepository {
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

    public PackageExecutionContext createPackageExecutionContext(ObjectEnricher enricher, ObjectPredicate predicate, String contextId, AutomationPackageFile apFile) {
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

        return new PackageExecutionContext(contextId, inMemoryPackageManager, false);
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
                IsolatedAutomationPackageRepository.log.info("Cleanup isolated execution context");

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
