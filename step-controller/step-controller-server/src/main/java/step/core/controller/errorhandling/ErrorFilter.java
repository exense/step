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
package step.core.controller.errorhandling;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceError;
import step.core.deployment.ControllerServiceException;
import step.framework.server.security.Secured;

@Secured
@Provider
public class ErrorFilter extends AbstractStepServices implements ExceptionMapper<Exception>  {

	private static final Logger logger = LoggerFactory.getLogger(ErrorFilter.class);
	
	@Override
	public Response toResponse(Exception exception) {
		if(exception instanceof ControllerServiceException) {
			ControllerServiceException portalException = (ControllerServiceException) exception;
			ControllerServiceError portalServiceError = new ControllerServiceError();
			portalServiceError.setErrorName(portalException.getErrorName());
			portalServiceError.setErrorMessage(portalException.getErrorMessage());
			return Response.status(portalException.getHttpErrorCode()).entity(portalServiceError)
					.type(MediaType.APPLICATION_JSON).build();
		} else {
			logger.error("Unexpected error while processing request", exception);
			return Response.status(500).entity("Unexpected server error occurred: "+exception.getClass().getName()+":"+exception.getMessage()).type("text/plain").build();
		}
	}
}
