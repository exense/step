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
package step.core.deployment;

import ch.exense.commons.app.Configuration;

import javax.annotation.PostConstruct;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class CORSRequestResponseFilter extends AbstractServices implements ContainerRequestFilter, ContainerResponseFilter {
	private String origin;

	@PostConstruct
	public void init() throws Exception {
		Configuration configuration = getContext().getConfiguration();
		origin = configuration.getProperty("frontend.baseUrl", "*");
	}
	
	/**
	 * Method for ContainerRequestFilter.
	 */
	@Override
	public void filter(ContainerRequestContext request) throws IOException {

		// If it's a preflight request, we abort the request with
		// a 200 status, and the CORS headers are added in the
		// response filter method below.
		if (isPreflightRequest(request)) {
			request.abortWith(Response.ok().build());
			return;
		}
	}

	/**
	 * A preflight request is an OPTIONS request
	 * with an Origin header.
	 */
	private static boolean isPreflightRequest(ContainerRequestContext request) {
		return request.getHeaderString("Origin") != null
				&& request.getMethod().equalsIgnoreCase("OPTIONS");
	}

	/**
	 * Method for ContainerResponseFilter.
	 */
	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response)
			throws IOException {

		// if there is no Origin header, then it is not a
		// cross origin request. We don't do anything.
		String origin = request.getHeaderString("Origin"); 
		if (origin == null) {
			return;
		}

		// If it is a preflight request, then we add all
		// the CORS headers here.
		if (isPreflightRequest(request)) {
			
		}
		//For now set it for all
		response.getHeaders().add("Access-Control-Allow-Credentials", "true");
		response.getHeaders().add("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS, HEAD");
		response.getHeaders().add("Access-Control-Allow-Headers",
				// Whatever other non-standard/safe headers (see list above) 
				// you want the client to be able to send to the server,
				// put it in this list. And remove the ones you don't want.
				"X-Requested-With, Authorization, " +
						"Accept-Version, Content-MD5, CSRF-Token, Content-Type, Cache-Control, " +
						"If-Modified-Since, Pragma");

		// Cross origin requests can be either simple requests
		// or preflight request. We need to add this header
		// to both type of requests. Only preflight requests
		// need the previously added headers.
		response.getHeaders().add("Access-Control-Allow-Origin", origin);
	}
}

