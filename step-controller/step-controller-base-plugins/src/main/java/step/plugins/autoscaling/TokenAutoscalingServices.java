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
package step.plugins.autoscaling;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingDriver;
import step.artefacts.handlers.functions.autoscaler.TokenProvisioningStatus;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.execution.ExecutionContext;
import step.framework.server.security.Secured;

@Singleton
@Path("/executions")
@Tag(name = "Executions")
public class TokenAutoscalingServices extends AbstractStepServices {

	private TokenAutoscalingDriver tokenAutoscalingDriver;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		tokenAutoscalingDriver = context.get(TokenAutoscalingDriver.class);
	}

	@GET
	@Path("/{executionId}/provisioning/status")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public TokenProvisioningStatus getAutoscalingStatus(@PathParam("executionId") String executionId) {
		ExecutionContext executionContext = getExecutionRunnable(executionId);
		if(executionContext != null) {
			String provisioningRequestId = TokenAutoscalingExecutionPlugin.getProvisioningRequestId(executionContext);
			if(provisioningRequestId != null) {
				return tokenAutoscalingDriver.getTokenProvisioningStatus(provisioningRequestId);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}