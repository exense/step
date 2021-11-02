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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.artefacts.AbstractArtefact;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Unfiltered;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;

@Singleton
@Path("plans")
public class PlanServices extends AbstractServices {

	protected PlanAccessor planAccessor;
	protected PlanTypeRegistry planTypeRegistry;
	protected ObjectPredicateFactory objectPredicateFactory;
	
	@PostConstruct
	public void init() {
		planAccessor = getContext().getPlanAccessor();
		planTypeRegistry = getContext().get(PlanTypeRegistry.class);
		objectPredicateFactory = getContext().get(ObjectPredicateFactory.class);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Unfiltered
	@Secured(right="plan-write")
	public Plan newPlan(@QueryParam("type") String type, @QueryParam("template") String template) throws Exception {
		PlanType<Plan> planType = planTypeRegistry.getPlanType(type);
		return planType.newPlan(template);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan save(Plan plan) {
		return planAccessor.save(plan);
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan get(@PathParam("id") String id) {
		return planAccessor.get(id);
	}
	
	@GET
	@Path("/{id}/compile")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public PlanCompilationResult compilePlan(@PathParam("id") String id) {
		Plan plan = planAccessor.get(id);
		PlanCompilationResult planCompilationResult = compilePlan(plan);
		if(!planCompilationResult.isHasError()) {
			save(plan);
		}
		return planCompilationResult;
	}
	
	@POST
	@Path("/compile")
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
	
	@POST
	@Path("/search")
	@Secured(right="plan-read")
	public Plan get(Map<String,String> attributes) {
		return planAccessor.findByAttributes(attributes);
	}

	@POST
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public List<Plan> findMany(Map<String,String> attributes) {
		return StreamSupport.stream(planAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
	}

	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public List<Plan> getAll(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return planAccessor.getRange(skip, limit);
		} else {
			return getAll(0, 1000);
		}
	}

	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-delete")
	public void delete(@PathParam("id") String id) {
		planAccessor.remove(new ObjectId(id));
	}
	
	@GET
	@Path("/{id}/artefacts/{artefactid}/lookup/plan")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan lookupPlan(@PathParam("id") String id, @PathParam("artefactid") String artefactId) {
		Plan plan = get(id);
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
	
	@POST
	@Path("/artefacts/clone")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	@Unfiltered
	public AbstractArtefact cloneArtefact(AbstractArtefact artefact) {
		assignNewId(artefact);
		return artefact;
	}
	
	@POST
	@Path("/artefacts/clonemany")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	@Unfiltered
	public List<AbstractArtefact> cloneArtefact(List<AbstractArtefact> artefacts) {
		return artefacts.stream().map(a->cloneArtefact(a)).collect(Collectors.toList());
	}
	
	private void assignNewId(AbstractArtefact artefact) {
		artefact.setId(new ObjectId());
		artefact.getChildren().forEach(a->assignNewId(a));
	}
	
}
