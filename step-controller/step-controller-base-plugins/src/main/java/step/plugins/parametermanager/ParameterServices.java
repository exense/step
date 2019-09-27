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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
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
	public void init() throws Exception {
		super.init();
		parameterAccessor = (CRUDAccessor<Parameter>) getContext().get("ParameterAccessor");
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="param-write")
	public Parameter newParameter(@Context ContainerRequestContext crc) {
		Parameter parameter =  new Parameter(new Expression(""), "", "", "");
		parameter.setPriority(1);
		if(hasGlobalParamRight(crc)) {
			parameter.setScope(ParameterScope.GLOBAL);
		} else {
			parameter.setScope(ParameterScope.FUNCTION);
		}
		return parameter;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@Secured(right="param-write")
	public Parameter save(Parameter newParameter, @Context ContainerRequestContext crc) {
		assertRights(newParameter, crc);
		
		Parameter oldParameter;
		if(newParameter.getId()!=null) {
			oldParameter = parameterAccessor.get(newParameter.getId());
		} else {
			oldParameter = null;
		}
		
		if(oldParameter == null){
			// new parameter. setting initial value of protected value.
			// values that contains password are protected
			newParameter.setProtectedValue(isPassword(newParameter));
		} else {
			// the parameter has been updated but the value hasn't been changed
			if(newParameter.getValue().equals(PROTECTED_VALUE)) {
				newParameter.setValue(oldParameter.getValue());
			}
			
			if(isProtected(oldParameter)) {
				// protected value should not be changed
				newParameter.setProtectedValue(true);
			} else {
				newParameter.setProtectedValue(isPassword(newParameter));
			}
		}

		return parameterAccessor.save(newParameter);
	}

	protected void assertRights(Parameter newParameter, ContainerRequestContext crc) {
		if(newParameter.getScope() == null || newParameter.getScope()==ParameterScope.GLOBAL) {
			if(!hasGlobalParamRight(crc)) {
				throw new RuntimeException("The user is missing the right 'param-global-write' to write global parameters.");
			}
		}
	}

	protected boolean hasGlobalParamRight(ContainerRequestContext crc) {
		return getSession(crc).getProfile().getRights().contains("param-global-write");
	}

	protected boolean isProtected(Parameter oldParameter) {
		return oldParameter.getProtectedValue()!=null && oldParameter.getProtectedValue();
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter copy(@PathParam("id") String id, @Context ContainerRequestContext crc) {	
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		parameter.setId(new ObjectId());
		return save(parameter, crc);
	}
	
	@DELETE
	@Path("/{id}")
	@Secured(right="param-delete")
	public void delete(@PathParam("id") String id, @Context ContainerRequestContext crc) {
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		assertRights(parameter, crc);
		
		parameterAccessor.remove(new ObjectId(id));
	}
	
	public static final String PROTECTED_VALUE = "******";
	

	public static boolean isPassword(Parameter parameter) {
		return parameter!=null && isPassword(parameter.getKey());
	}
	
	public static boolean isPassword(String key) {
		return key!=null && (key.contains("pwd")||key.contains("password"));
	}
	
	@GET
	@Path("/{id}")
	@Secured(right="param-read")
	public Parameter get(@PathParam("id") String id) {
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		if(parameter!=null) {
			maskProtectedValue(parameter);
		}
		return parameter;
	}
	
	protected Parameter maskProtectedValue(Parameter parameter) {
		if(isProtected(parameter)) {
			parameter.setValue(PROTECTED_VALUE);				
		}
		return parameter;
	}
	
	@GET
	@Path("/all")
	@Secured(right="param-read")
	public List<Parameter> getAll() {
		List<Parameter> result = new ArrayList<>();
		parameterAccessor.getAll().forEachRemaining(p->{
			result.add(maskProtectedValue(p));
		});
		
		return result;
	}
}
