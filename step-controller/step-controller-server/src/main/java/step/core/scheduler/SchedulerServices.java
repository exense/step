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
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Session;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Singleton
@Path("scheduler")
@Tag(name = "Scheduler")
public class SchedulerServices extends AbstractServices {

    private ExecutionScheduler scheduler;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        scheduler = getScheduler();
    }

    @Operation(description = "Returns a new scheduler task instance as template. This instance will have to be saved using the dedicated service.")
    @GET
    @Path("/task/new")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "task-write")
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

    @Operation(description = "Create or update a scheduler task.")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/task")
    @Secured(right = "task-write")
    public void schedule(ExecutiontTaskParameters schedule) {
        // Enrich the execution parameters with the attributes of the task parameters.
        // The attributes of the execution parameters are then added to the Execution
        // This is for instance needed to run the execution within the same project as
        // the scheduler task
        getObjectEnricher().accept(schedule.getExecutionsParameters());
        scheduler.addExecutionTask(schedule);
    }

    @Operation(description = "Returns the scheduler task for the given ID.")
    @GET
    @Path("/task/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "task-read")
    public ExecutiontTaskParameters getExecutionTask(@PathParam("id") String executionTaskID) {
        return scheduler.get(executionTaskID);
    }

    @Operation(description = "Returns all the scheduled tasks.")
    @GET
    @Path("/task")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "task-read")
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
    @Path("/task/{id}/execute")
    @Secured(right = "plan-execute")
    public String execute(@PathParam("id") String executionTaskID) {
        Session session = getSession();
        return scheduler.executeExecutionTask(executionTaskID, session.getUser().getUsername());
    }

    @Operation(description = "Enable/disable all the scheduler tasks.")
    @PUT
    @Path("/task/schedule")
    @Secured(right = "admin")
    public void enableAllExecutionTasksSchedule(@QueryParam("enabled") Boolean enabled) {
        if (enabled != null && enabled) {
            scheduler.enableAllExecutionTasksSchedule();
        } else {
            scheduler.disableAllExecutionTasksSchedule();
        }
    }

    @Operation(description = "Enable/disable the given scheduler task.")
    @PUT
    @Path("/task/{id}")
    @Secured(right = "task-write")
    public void enableExecutionTask(@PathParam("id") String executionTaskID) {
        scheduler.enableExecutionTask(executionTaskID);
    }

    @Operation(description = "Remove or disable the given scheduler task, depending on the 'remove' parameter.")
    @DELETE
    @Path("/task/{id}")
    @Secured(right = "task-delete")
    public void removeExecutionTask(@PathParam("id") String executionTaskID, @QueryParam("remove") Boolean remove) {
        if (remove != null && remove) {
            scheduler.removeExecutionTask(executionTaskID);
        } else {
            scheduler.disableExecutionTask(executionTaskID);
        }
    }

}
