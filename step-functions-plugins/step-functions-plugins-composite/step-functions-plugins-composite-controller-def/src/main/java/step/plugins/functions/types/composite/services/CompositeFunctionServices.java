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
package step.plugins.functions.types.composite.services;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanType;
import step.core.plans.PlanTypeRegistry;
import step.framework.server.security.Secured;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.plugins.functions.types.CompositeFunction;

@Singleton
@Path("composites")
@Tag(name = "Composites")
public class CompositeFunctionServices extends AbstractStepServices {

    private FunctionAccessor functionAccessor;
    private  PlanTypeRegistry planTypeRegistry;
    private PlanAccessor planAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        functionAccessor = getContext().get(FunctionAccessor.class);
        planAccessor = getContext().getPlanAccessor();
        planTypeRegistry = context.get(PlanTypeRegistry.class);
    }

    @Operation(operationId = "cloneCompositePlan", description = "Clones the plan of the composite to a new plan")
    @GET
    @Path("/{id}/clone/plan")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "plan-write")
    public Plan clone(@PathParam("id") String id) {
        Function function = functionAccessor.get(id);
        if (function == null) {
            throw new ControllerServiceException("The keyword with Id " + id + " doesn't exist");
        }
        if (! (function instanceof CompositeFunction)) {
            throw new ControllerServiceException("The function with Id " + id + " is not a Composite keyword");
        }

        CompositeFunction composite = (CompositeFunction) function;
        Plan plan = composite.getPlan();
        PlanType<Plan> planType = (PlanType<Plan>) planTypeRegistry.getPlanType(plan.getClass());
        Plan clonePlan = planType.clonePlan(plan, true);
        assignNewId(clonePlan.getRoot());
        //enrich the new plan
        getObjectEnricher().accept(clonePlan);
        planAccessor.save(clonePlan);
        return clonePlan;
    }

    private void assignNewId(AbstractArtefact artefact) {
        artefact.setId(new ObjectId());
        artefact.getChildren().forEach(this::assignNewId);
    }
}
