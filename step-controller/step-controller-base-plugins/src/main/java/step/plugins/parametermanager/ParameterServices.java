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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.parameter.Parameter;
import step.commons.activation.Expression;
import step.core.GlobalContext;
import step.core.access.AccessManager;
import step.core.accessors.CRUDAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.parameter.ParameterScope;

@Path("/parameters")
public class ParameterServices extends AbstractServices {
	
	private AccessManager accessManager;
	private CRUDAccessor<Parameter> parameterAccessor;
	
	@PostConstruct
	@SuppressWarnings("unchecked")
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		parameterAccessor = (CRUDAccessor<Parameter>) context.get("ParameterAccessor");
		accessManager = context.get(AccessManager.class);
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter newParameter() {
		Parameter parameter =  new Parameter(new Expression(""), "", "", "");
		parameter.setPriority(1);
		if(hasGlobalParamRight()) {
			parameter.setScope(ParameterScope.GLOBAL);
		} else {
			parameter.setScope(ParameterScope.FUNCTION);
		}
		return parameter;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter save(Parameter newParameter) {
		assertRights(newParameter);
		
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
		
		ParameterScope scope = newParameter.getScope();
		if(scope != null && scope.equals(ParameterScope.GLOBAL)) {
			newParameter.setScopeEntity(null);
		}

		String lastModificationUser = getSession().getUser().getUsername();
		Date lastModificationDate = new Date();
		newParameter.setLastModificationDate(lastModificationDate);
		newParameter.setLastModificationUser(lastModificationUser);
		
		return parameterAccessor.save(newParameter);
	}

	protected void assertRights(Parameter newParameter) {
		if(newParameter.getScope() == null || newParameter.getScope()==ParameterScope.GLOBAL) {
			if(!hasGlobalParamRight()) {
				throw new RuntimeException("The user is missing the right 'param-global-write' to write global parameters.");
			}
		}
	}

	protected boolean hasGlobalParamRight() {
		return accessManager.checkRightInContext(getSession(), "param-global-write");
	}

	protected boolean isProtected(Parameter oldParameter) {
		return oldParameter.getProtectedValue()!=null && oldParameter.getProtectedValue();
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
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		assertRights(parameter);
		
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

	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public Parameter get(Map<String,String> attributes) {
		return parameterAccessor.findByAttributes(attributes);
	}

	@POST
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public List<Parameter> findMany(Map<String,String> attributes) {
		return StreamSupport.stream(parameterAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public List<Parameter> getAll(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return parameterAccessor.getRange(skip, limit);
		} else {
			return getAll(0, 1000);
		}
	}
}
