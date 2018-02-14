/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.parametermanager;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.commons.activation.Expression;
import step.core.accessors.CRUDAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;

@Path("/parameters")
public class ParameterServices extends AbstractServices {
	
	CRUDAccessor<Parameter> parameterAccessor;
	
	@PostConstruct
	@SuppressWarnings("unchecked")
	public void init() {
		parameterAccessor = (CRUDAccessor<Parameter>) getContext().get("ParameterAccessor");
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="param-write")
	public Parameter newParameter() {
		return  new Parameter(new Expression(""), "", "", "");
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="param-write")
	public Parameter save(Parameter parameter) {
		return parameterAccessor.save(parameter);
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter copy(@PathParam("id") String id) {	
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		parameter.setId(new ObjectId());
		return save(parameter);
	}
	
	@DELETE
	@Path("/{id}")
	@Secured(right="param-delete")
	public void delete(@PathParam("id") String id) {
		parameterAccessor.remove(new ObjectId(id));
	}
	
	@GET
	@Path("/{id}")
	@Secured(right="param-read")
	public Parameter get(@PathParam("id") String id) {
		return parameterAccessor.get(new ObjectId(id));
	}
}
