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
package step.controller.services.async;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;

@Singleton
@Path("async")
@Tag(name = "Async Tasks")
public class AsyncTaskServices extends AbstractStepServices {

    private AsyncTaskManager asyncTaskManager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        asyncTaskManager = context.require(AsyncTaskManager.class);
    }

    @Operation(description = "Retrieve the status of an async task by its Id")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    public AsyncTaskStatus<?> getAsyncTaskStatus(@PathParam("id") String id) {
        return asyncTaskManager.getAsyncTaskStatus(id);
    }
}
