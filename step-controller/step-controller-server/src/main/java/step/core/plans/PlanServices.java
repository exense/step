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
package step.core.plans;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.controller.services.bulk.BulkOperationManager;
import step.controller.services.bulk.BulkOperationParameters;
import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.collections.Collection;
import step.core.deployment.AbstractStepServices;
import step.core.export.ExportStatus;
import step.core.export.ExportTaskManager;
import step.core.objectenricher.ObjectFilter;
import step.framework.server.security.Secured;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Path("plans")
@Tag(name = "Plans")
public class PlanServices extends AbstractStepServices {

	protected PlanAccessor planAccessor;
	protected PlanTypeRegistry planTypeRegistry;
	protected ObjectPredicateFactory objectPredicateFactory;
	private ArtefactHandlerRegistry artefactHandlerRegistry;
	protected ExportTaskManager exportTaskManager;
	private BulkOperationManager bulkOperationManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		planAccessor = context.getPlanAccessor();
		planTypeRegistry = context.get(PlanTypeRegistry.class);
		objectPredicateFactory = context.get(ObjectPredicateFactory.class);
		artefactHandlerRegistry = context.getArtefactHandlerRegistry();
		exportTaskManager = context.require(ExportTaskManager.class);
		bulkOperationManager = new BulkOperationManager(exportTaskManager);
	}

	@Operation(description = "Returns a new plan instance as template.")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan newPlan(@QueryParam("type") String type, @QueryParam("template") String template) throws Exception {
		PlanType<Plan> planType = planTypeRegistry.getPlanType(type);
		Plan plan = planType.newPlan(template);
		getObjectEnricher().accept(plan);
		return plan;
	}

	@Operation(description = "Creates / updates the given plan.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan savePlan(Plan plan) {
		@SuppressWarnings("unchecked")
		PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(plan.getClass());
		planType.onBeforeSave(plan);
		return planAccessor.save(plan);
	}

	@Operation(description = "Returns the plan with the given id.")
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan getPlanById(@PathParam("id") String id) {
		return planAccessor.get(id);
	}

	@Operation(description = "Compiles the plan with the given id.")
	@GET
	@Path("/{id}/compile")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public PlanCompilationResult compilePlanWithId(@PathParam("id") String id) {
		Plan plan = planAccessor.get(id);
		PlanCompilationResult planCompilationResult = compilePlan(plan);
		if(!planCompilationResult.isHasError()) {
			savePlan(plan);
		}
		return planCompilationResult;
	}

	@Operation(description = "Compiles the provided plan.")
	@POST
	@Path("/compile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public PlanCompilationResult compilePlan(Plan plan) {
		@SuppressWarnings("unchecked")
		PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(plan.getClass());
		PlanCompiler<Plan> planCompiler = planType.getPlanCompiler();
		PlanCompilationResult planCompilationResult = new PlanCompilationResult();
		try {
			plan = planCompiler.compile(plan);
			planCompilationResult.setPlan(plan);
		} catch(PlanCompilerException e) {
			planCompilationResult.setHasError(true);
			planCompilationResult.setErrors(e.getErrors());
		}
		return planCompilationResult;
	}

	@Operation(description = "Clones and returns the plan with the given id. The result of this method will have to be saved with the dedicated method.")
	@GET
	@Path("/{id}/clone")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan clonePlan(@PathParam("id") String id) {
		Plan plan = planAccessor.get(id);
		@SuppressWarnings("unchecked")
		PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(plan.getClass());
		Plan clonePlan = planType.clonePlan(plan);
		assignNewId(clonePlan.getRoot());
		return clonePlan;
	}

	@Operation(description = "Bulk clone plans according to the provided parameters")
	@POST
	@Path("/bulk/clone")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public ExportStatus clonePlans(BulkOperationParameters parameters) {
		ObjectFilter contextObjectFilter = getObjectFilter();
		Collection<Plan> collection = planAccessor.getCollectionDriver();
		return bulkOperationManager.performBulkOperation(parameters, this::clonePlan, filter -> {
			collection.find(filter, null, null, null, 0).forEach(plan -> clonePlan(plan.getId().toString()));
		}, contextObjectFilter);
	}

	@Operation(description = "Returns the first plan matching the given attributes.")
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan getPlanByAttributes(Map<String,String> attributes) {
		return planAccessor.findByAttributes(attributes);
	}

	@Operation(description = "Returns the plans matching the given attributes.")
	@POST
	@Path("/find")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public List<Plan> findPlansByAttributes(Map<String,String> attributes) {
		return StreamSupport.stream(planAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
	}

	@Operation(description = "Returns all the plans.")
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public List<Plan> getAllPlans(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return planAccessor.getRange(skip, limit);
		} else {
			return getAllPlans(0, 1000);
		}
	}

	@Operation(description = "Deletes the plan with the given id.")
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-delete")
	public void deletePlan(@PathParam("id") String id) {
		planAccessor.remove(new ObjectId(id));
	}

	@Operation(description = "Bulk delete plans according to the provided parameters")
	@POST
	@Path("/bulk/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-delete")
	public ExportStatus deletePlans(BulkOperationParameters parameters) {
		ObjectFilter contextObjectFilter = getObjectFilter();
		Collection<Plan> collection = planAccessor.getCollectionDriver();
		return bulkOperationManager.performBulkOperation(parameters, this::deletePlan, collection::remove, contextObjectFilter);
	}

	@Operation(description = "Returns the plan referenced by the given artifact within the given plan.")
	@GET
	@Path("/{id}/artefacts/{artefactid}/lookup/plan")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan lookupPlan(@PathParam("id") String id, @PathParam("artefactid") String artefactId) {
		Plan plan = getPlanById(id);
		Plan result = null;
		PlanNavigator planNavigator = new PlanNavigator(plan);
		CallPlan artefact = (CallPlan) planNavigator.findArtefactById(artefactId);
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		PlanLocator planLocator = new PlanLocator(getContext().getPlanAccessor(), selectorHelper);
		ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
		try {
			result = planLocator.selectPlan(artefact, objectPredicate, null);
		} catch (RuntimeException e) {}
		return result;
	}

	@Operation(description = "Clones the provided artefact.")
	@POST
	@Path("/artefacts/clone")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public AbstractArtefact cloneArtefact(AbstractArtefact artefact) {
		assignNewId(artefact);
		return artefact;
	}

	@Operation(description = "Clones the provided artefacts.")
	@POST
	@Path("/artefacts/clonemany")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public List<AbstractArtefact> cloneArtefacts(List<AbstractArtefact> artefacts) {
		return artefacts.stream().map(a->cloneArtefact(a)).collect(Collectors.toList());
	}

	@Operation(description = "Returns the supported artefact types.")
	@GET
	@Path("/artefact/types")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Set<String> getArtefactTypes() {
		return artefactHandlerRegistry.getArtefactNames();
	}

	@Operation(description = "Returns the artefact with the given id.")
	@GET
	@Path("/artefact/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public AbstractArtefact getArtefactType(@PathParam("id") String type) throws Exception {
		return artefactHandlerRegistry.getArtefactTypeInstance(type);
	}

	@Operation(description = "Returns the names of the supported artefacts.")
	@GET
	@Path("/artefact/templates")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Set<String> getArtefactTemplates() {
		return new TreeSet<>(artefactHandlerRegistry.getArtefactTemplateNames());
	}
	
	private void assignNewId(AbstractArtefact artefact) {
		artefact.setId(new ObjectId());
		artefact.getChildren().forEach(a->assignNewId(a));
	}
	
}
