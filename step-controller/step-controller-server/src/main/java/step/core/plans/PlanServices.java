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
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.controller.services.entities.AbstractEntityServices;
import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.deployment.ControllerServiceException;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.entities.EntityManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;
import step.plans.parser.yaml.YamlPlanReader;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Singleton
@Path("plans")
@Tag(name = "Plans")
@Tag(name = "Entity=Plan")
@SecuredContext(key = "entity", value = "plan")
public class PlanServices extends AbstractEntityServices<Plan> {

	private static final Logger log = LoggerFactory.getLogger(PlanServices.class);

	protected PlanAccessor planAccessor;
	protected PlanTypeRegistry planTypeRegistry;
	protected ObjectPredicateFactory objectPredicateFactory;
	private ArtefactHandlerRegistry artefactHandlerRegistry;

	private final YamlPlanReader yamlPlanReader = new YamlPlanReader();

	public PlanServices() {
		super(EntityManager.plans);
	}

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		planAccessor = context.getPlanAccessor();
		planTypeRegistry = context.get(PlanTypeRegistry.class);
		objectPredicateFactory = context.get(ObjectPredicateFactory.class);
		artefactHandlerRegistry = context.getArtefactHandlerRegistry();
	}

	@Operation(description = "Returns a new plan instance as template.")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-write")
	public Plan newPlan(@QueryParam("type") String type, @QueryParam("template") String template, @QueryParam("name") String name) throws Exception {
		PlanType<Plan> planType = getPlanTypeNotNull(type);
		Plan plan = planType.newPlan(template, name);
		getObjectEnricher().accept(plan);
		return plan;
	}

	@Operation(description = "Returns a new plan instance created from the yaml source.")
	@POST
	@Path("/yaml")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	@Secured(right = "{entity}-write")
	public Plan newPlanFromYaml(String yamlBody) {
		try {
			return yamlPlanReader.readYamlPlan(new ByteArrayInputStream(yamlBody.getBytes()));
		} catch (Exception e) {
			log.error("Deserialization error while reading providing YAML plan.", e);
			throw new ControllerServiceException("Deserialization error while reading the Yaml plan: " + e.getMessage(), e);
		}
	}

	@Override
	protected Plan beforeSave(Plan entity) {
		if (entity == null) {
			throw new ControllerServiceException(400, "The submitted plan to be saved is empty or null");
		}
		PlanType<Plan> planType = getPlanTypeNotNull(entity.getClass());
		planType.onBeforeSave(entity);
		return super.beforeSave(entity);
	}

	protected PlanType<Plan> getPlanTypeNotNull(String planTypeName) {
		PlanType<Plan> planType = planTypeRegistry.getPlanType(planTypeName);
		if (planType == null) {
			throw new ControllerServiceException(400, "Plan type " + planTypeName + " is not supported");
		}
		return planType;
	}

	protected PlanType<Plan> getPlanTypeNotNull(Class<? extends Plan> entityClass) {
		PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(entityClass);
		if (planType == null) {
			throw new ControllerServiceException(400, "Plan type is not resolved for entity class " + entityClass);
		}
		return planType;
	}

	@Operation(description = "Compiles the plan with the given id.")
	@GET
	@Path("/{id}/compile")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-write")
	public PlanCompilationResult compilePlanWithId(@PathParam("id") String id) throws Exception {
		Plan plan = planAccessor.get(id);
		PlanCompilationResult planCompilationResult = compilePlan(plan);
		if(!planCompilationResult.isHasError()) {
			save(plan);
		}
		return planCompilationResult;
	}

	@Operation(description = "Compiles the provided plan.")
	@POST
	@Path("/compile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-write")
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

	@Override
	protected Plan cloneEntity(Plan plan) {
		// Delegate clone to plan type manager
		@SuppressWarnings("unchecked")
		PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(plan.getClass());
		Plan clonePlan = planType.clonePlan(plan, true);
		assignNewId(clonePlan.getRoot());
		return clonePlan;
	}

	@Operation(description = "Returns the first plan matching the given attributes.")
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Plan getPlanByAttributes(Map<String,String> attributes) {
		return planAccessor.findByAttributes(attributes);
	}

	@Operation(description = "Returns all the plans.")
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public List<Plan> getAllPlans(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return planAccessor.getRange(skip, limit);
		} else {
			return getAllPlans(0, 1000);
		}
	}

	@Operation(description = "Returns the plan referenced by the given artifact within the given plan.")
	@GET
	@Path("/{id}/artefacts/{artefactid}/lookup/plan")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
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

	@Operation(description = "Returns the plan referenced by the given CallPlan.")
	@POST
	@Path("/lookup")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Plan lookupCallPlan(CallPlan callPlan) {
		Plan result = null;
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		PlanLocator planLocator = new PlanLocator(getContext().getPlanAccessor(), selectorHelper);
		ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
		try {
			result = planLocator.selectPlan(callPlan, objectPredicate, null);
		} catch (RuntimeException e) {}
		return result;
	}

	@Operation(description = "Clones the provided artefact.")
	@POST
	@Path("/artefacts/clone")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-write")
	public AbstractArtefact cloneArtefact(AbstractArtefact artefact) {
		assignNewId(artefact);
		return artefact;
	}

	@Operation(description = "Clones the provided artefacts.")
	@POST
	@Path("/artefacts/clonemany")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-write")
	public List<AbstractArtefact> cloneArtefacts(List<AbstractArtefact> artefacts) {
		return artefacts.stream().map(this::cloneArtefact).collect(Collectors.toList());
	}

	@Operation(description = "Returns the list of artefact types that can be used as control within Plans")
	@GET
	@Path("/artefact/types")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Set<String> getArtefactTypes() {
		return artefactHandlerRegistry.getControlArtefactNames();
	}

	@Operation(description = "Returns the artefact with the given id.")
	@GET
	@Path("/artefact/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public AbstractArtefact getArtefactType(@PathParam("id") String type) throws Exception {
		return artefactHandlerRegistry.getArtefactTypeInstance(type);
	}

	@Operation(description = "Returns the list of artefact types that can be used as root element of Plans.")
	@GET
	@Path("/artefact/templates")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Set<String> getArtefactTemplates() {
		return new TreeSet<>(artefactHandlerRegistry.getRootArtefactNames());
	}


	@Operation(description = "Returns the plan in yaml format.")
	@GET
	@Path("/{id}/yaml")
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(right="{entity}-read")
	public Response getYamlPlan(@PathParam("id") String id) {
		Plan plan = planAccessor.get(id);
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) {
				try {
					yamlPlanReader.writeYamlPlan(output, plan);
				} catch (Exception ex) {
					log.error("Serialization error", ex);
					throw new ControllerServiceException("Serialization error when converting to YAML plan, check the controller logs for more details", ex);
				}
			}
		};

		String mimeType = "text/plain; charset=utf-8";
		return Response.ok(fileStream, mimeType).build();

	}

	private void assignNewId(AbstractArtefact artefact) {
		artefact.setId(new ObjectId());
		artefact.getChildren().forEach(this::assignNewId);
	}

}
