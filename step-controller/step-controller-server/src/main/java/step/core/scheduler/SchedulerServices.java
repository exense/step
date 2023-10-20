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
package step.core.scheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.controller.services.entities.AbstractEntityServices;
import step.core.access.User;
import step.core.deployment.ControllerServiceException;
import step.core.entities.EntityManager;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;
import step.framework.server.Session;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Singleton
@Path("scheduler/task")
@Tag(name = "Scheduler")
@Tag(name = "Entity=ExecutionTask")
@SecuredContext(key = "entity", value = "task")
public class SchedulerServices extends AbstractEntityServices<ExecutiontTaskParameters> {

    private ExecutionScheduler scheduler;

    public SchedulerServices() {
        super(EntityManager.tasks);
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        scheduler = getScheduler();
    }

    @Operation(description = "Returns a new scheduler task instance as template. This instance will have to be saved using the dedicated service.")
    @GET
    @Path("/new")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public ExecutiontTaskParameters createExecutionTask() {
        ExecutiontTaskParameters taskParameters = new ExecutiontTaskParameters();
        taskParameters.setActive(true);
        ExecutionParameters executionsParameters = new ExecutionParameters();
        HashMap<String, String> repositoryParameters = new HashMap<>();
        executionsParameters.setRepositoryObject(new RepositoryObjectReference("local", repositoryParameters));
        executionsParameters.setMode(ExecutionMode.RUN);
        taskParameters.setExecutionsParameters(executionsParameters);
        getObjectEnricher().accept(taskParameters);
        return taskParameters;
    }

    @Override
    public ExecutiontTaskParameters save(ExecutiontTaskParameters schedule) {
        // Enrich the execution parameters with the attributes of the task parameters.
        // The attributes of the execution parameters are then added to the Execution
        // This is for instance needed to run the execution within the same project as
        // the scheduler task
        getObjectEnricher().accept(schedule.getExecutionsParameters());

        // when create/update the execution task, we need to check that user defined in execution parameters have rights to execute it
        checkRightsOnBehalfOf("plan-execute", schedule.getExecutionsParameters().getUserID());

        try {
            scheduler.addExecutionTask(schedule);
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
        return schedule;
    }

    @Operation(description = "Returns all the scheduled tasks.")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<ExecutiontTaskParameters> getScheduledExecutions() {
        List<ExecutiontTaskParameters> result = new ArrayList<>();
        Iterator<ExecutiontTaskParameters> it = scheduler.getActiveAndInactiveExecutionTasks();
        int maxSize = getContext().getConfiguration().getPropertyAsInteger("tec.services.tasks.maxsize", 500);
        while (it.hasNext() && result.size() < maxSize) {
            result.add(it.next());
        }
        return result;
    }

    @Operation(description = "Execute the given scheduler task.")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{id}/execute")
    @Secured(right = "plan-execute")
    public String executeTask(@PathParam("id") String executionTaskID) {
        Session<User> session = getSession();
        return scheduler.executeExecutionTask(executionTaskID, session.getUser().getUsername());
    }

    @Operation(description = "Enable/disable all the scheduler tasks.")
    @PUT
    @Path("/schedule")
    @Secured(right = "scheduler-manage")
    public void enableAllExecutionTasksSchedule(@QueryParam("enabled") Boolean enabled) {
        if (enabled != null && enabled) {
            scheduler.enableAllExecutionTasksSchedule();
        } else {
            scheduler.disableAllExecutionTasksSchedule();
        }
    }

    @Operation(description = "Enable/disable the given scheduler task.")
    @PUT
    @Path("/{id}")
    @Secured(right = "{entity}-write")
    public void enableExecutionTask(@PathParam("id") String executionTaskID, @QueryParam("enabled") Boolean enabled) {
        try {
            if (enabled != null && enabled) {
                scheduler.enableExecutionTask(executionTaskID);
            } else {
                scheduler.disableExecutionTask(executionTaskID);
            }
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @Override
    public void delete(String id) {
        scheduler.removeExecutionTask(id);
    }
}
