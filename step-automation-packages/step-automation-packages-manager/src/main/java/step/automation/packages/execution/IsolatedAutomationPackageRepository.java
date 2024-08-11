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

import ch.exense.commons.io.FileHelper;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageManagerException;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.LayeredResourceManager;
import step.resources.ResourceManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IsolatedAutomationPackageRepository extends AbstractRepository {

    public static final String REPOSITORY_PARAM_CONTEXTID = "contextid";

    public static final Logger log = LoggerFactory.getLogger(IsolatedAutomationPackageRepository.class);

    // context id -> automation package manager (cache)
    private final ConcurrentHashMap<String, PackageExecutionContext> sharedPackageExecutionContexts = new ConcurrentHashMap<>();

    // TODO: use persistent storage with cleanup
    // context id -> File
    private final ConcurrentHashMap<String, File> apFiles = new ConcurrentHashMap<>();

    private final AutomationPackageManager manager;
    private final FunctionTypeRegistry functionTypeRegistry;
    private final FunctionAccessor functionAccessor;

    protected IsolatedAutomationPackageRepository(AutomationPackageManager manager, 
                                                  FunctionTypeRegistry functionTypeRegistry, 
                                                  FunctionAccessor functionAccessor) {
        super(Set.of(REPOSITORY_PARAM_CONTEXTID));
        this.manager = manager;
        this.functionTypeRegistry = functionTypeRegistry;
        this.functionAccessor = functionAccessor;
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
        // we expect, that there is only one automation package stored per context
        PackageExecutionContext ctx = getOrRestorePackageExecutionContext(repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID), null, null);
        try {
            AutomationPackage automationPackage = ctx.getAutomationPackage();
            if (automationPackage == null) {
                return null;
            }
            ArtefactInfo info = new ArtefactInfo();
            info.setType("automationPackage");
            info.setName(automationPackage.getAttribute(AbstractOrganizableObject.NAME));
            return info;
        } finally {
            if (!ctx.isExternallyCreatedContext()) {
                ctx.close();
            }
        }
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate) throws Exception {
        return new TestSetStatusOverview();
    }

    private PackageExecutionContext getOrRestorePackageExecutionContext(String contextId, ObjectEnricher enricher, ObjectPredicate predicate) {
        // Execution context can be created in-advance and shared between several plans
        PackageExecutionContext current = sharedPackageExecutionContexts.get(contextId);
        if (current == null) {
            // But in case of re-run for local plan it can be not yet prepared
            // Here we resolve the original AP file used for previous isolated execution and re-use it to create the execution context

            // TODO: store files not by contextId but for ap name
            File apFile = apFiles.get(contextId);
            if (apFile == null) {
                throw new AutomationPackageManagerException("AP file is not stored for execution context " + contextId);
            }

            // prepare the isolated in-memory automation package manager with the only one automation package
            AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                    new ObjectId(contextId), functionTypeRegistry,
                    functionAccessor
            );

            // create single automation package in isolated manager
            try (FileInputStream fis = new FileInputStream(apFile)) {
                inMemoryPackageManager.createAutomationPackage(fis, apFile.getName(), enricher, predicate);
            } catch (IOException e) {
                throw new AutomationPackageManagerException("Cannot read the AP file " + apFile.getName());
            }

            return new PackageExecutionContext(contextId, inMemoryPackageManager, false);
        }
        return current;
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws IOException {
        PackageExecutionContext ctx = null;

        try {
            ctx = getOrRestorePackageExecutionContext(repositoryParameters.get(REPOSITORY_PARAM_CONTEXTID), context.getObjectEnricher(), context.getObjectPredicate());
            AutomationPackage automationPackage = ctx.getAutomationPackage();

            // PLAN_NAME but not PLAN_ID is used, because plan id is not persisted for isolated execution
            // (it is impossible to re-run the execution by plan id)
            String planName = repositoryParameters.get(RepositoryObjectReference.PLAN_NAME);
            ImportResult result = new ImportResult();

            AutomationPackageManager apManager = ctx.getInMemoryManager();
            Plan plan = apManager.getPackagePlans(automationPackage.getId())
                    .stream()
                    .filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(planName)).findFirst().orElse(null);
            if (plan == null) {
                // failed result
                result.setErrors(List.of("Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " has no plan with name=" + planName));
                return result;
            }
            result.setPlanId(plan.getId().toString());
            enrichPlan(context, plan);

            PlanAccessor planAccessor = context.getPlanAccessor();
            if (!isLayeredAccessor(planAccessor)) {
                result.setErrors(List.of(planAccessor.getClass() + " is not layered"));
                return result;
            }

            planAccessor.save(plan);

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
        } finally {
            // if the context is created externally (shared for several plans), it should be managed (closed) in the calling code
            if (ctx != null && !ctx.isExternallyCreatedContext()) {
                ctx.close();
            }
        }
    }

    private static boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {

    }

    public PackageExecutionContext createPackageExecutionContext(String contextId, InputStream apStream, String fileName, ObjectEnricher enricher, ObjectPredicate predicate) {
        // store file in temporary storage to support rerun
        // TODO: temp solution - finally we need rewrite files for the same automation package (don't store separate file per context)
        File file = null;
        try {
            file = copyStreamToTempFile(apStream, fileName);
            this.apFiles.put(contextId, file);
        } catch (IOException ex) {
            throw new AutomationPackageManagerException("Cannot execute automation package " + fileName, ex);
        }

        // prepare the isolated in-memory automation package manager with the only one automation package
        AutomationPackageManager inMemoryPackageManager = manager.createIsolated(
                new ObjectId(contextId), functionTypeRegistry,
                functionAccessor
        );

        // create single automation package in isolated manager
        inMemoryPackageManager.createAutomationPackage(apStream, fileName, enricher, predicate);

        PackageExecutionContext ctx = new PackageExecutionContext(contextId, inMemoryPackageManager, true);
        sharedPackageExecutionContexts.put(contextId, ctx);
        return ctx;
    }

    // TODO: temp solution
    private File copyStreamToTempFile(InputStream in, String fileName) throws IOException {
        // create temp folder to keep the original file name
        File newFolder = FileHelper.createTempFolder();
        newFolder.deleteOnExit();
        File newFile = new File(newFolder, fileName);
        newFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            IOUtils.copy(in, out);
        }
        return newFile;
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

                PackageExecutionContext automationPackageManager = sharedPackageExecutionContexts.get(contextId);
                if (automationPackageManager != null) {
                    automationPackageManager.getInMemoryManager().cleanup();
                }
            } finally {
                sharedPackageExecutionContexts.remove(contextId);
            }
        }
    }
}
