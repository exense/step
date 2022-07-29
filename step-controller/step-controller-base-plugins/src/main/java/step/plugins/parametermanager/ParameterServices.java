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
package step.plugins.parametermanager;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;

import step.commons.activation.Expression;
import step.core.GlobalContext;
import step.core.accessors.Accessor;
import step.core.deployment.AbstractStepServices;
import step.core.encryption.EncryptionManagerException;
import step.framework.server.access.AccessManager;
import step.framework.server.security.Secured;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;

@Path("/parameters")
@Tag(name = "Parameters")
public class ParameterServices extends AbstractStepServices {
	
	private AccessManager accessManager;
	private Accessor<Parameter> parameterAccessor;
	private ParameterManager parameterManager;
	
	@PostConstruct
	@SuppressWarnings("unchecked")
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		parameterAccessor = (Accessor<Parameter>) context.get("ParameterAccessor");
		parameterManager = context.require(ParameterManager.class);
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
		getObjectEnricher().accept(parameter);
		return parameter;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter save(Parameter newParameter) throws EncryptionManagerException {
		Parameter oldParameter;
		if(newParameter.getId()!=null) {
			oldParameter = parameterAccessor.get(newParameter.getId());
		} else {
			oldParameter = null;
		}
		
		return save(newParameter, oldParameter);
	}
	
	private Parameter save(Parameter newParameter, Parameter sourceParameter) throws EncryptionManagerException {
		assertRights(newParameter);
		
		if(sourceParameter == null){
			// new parameter. setting initial value of protected value.
			// values that contains password are protected
			newParameter.setProtectedValue(isPassword(newParameter));
		} else {
			// the parameter has been updated but the value hasn't been changed
			if(newParameter.getValue().equals(PROTECTED_VALUE)) {
				newParameter.setValue(sourceParameter.getValue());
			}
			
			if(isProtected(sourceParameter)) {
				// protected value should not be changed
				newParameter.setProtectedValue(true);
			} else {
				newParameter.setProtectedValue(isPassword(newParameter));
			}
		}
		
		newParameter = parameterManager.encryptParameterValueIfEncryptionManagerAvailable(newParameter);
		
		ParameterScope scope = newParameter.getScope();
		if(scope != null && scope.equals(ParameterScope.GLOBAL)) {
			newParameter.setScopeEntity(null);
		}

		String lastModificationUser = getSession().getUser().getUsername();
		Date lastModificationDate = new Date();
		newParameter.setLastModificationDate(lastModificationDate);
		newParameter.setLastModificationUser(lastModificationUser);
		
		return maskProtectedValue(parameterAccessor.save(newParameter));
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

	protected static boolean isProtected(Parameter oldParameter) {
		return oldParameter.getProtectedValue()!=null && oldParameter.getProtectedValue();
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-write")
	public Parameter copy(@PathParam("id") String id) throws EncryptionManagerException {
		Parameter sourceParameter = parameterAccessor.get(new ObjectId(id));
		// Create a clone of the source parameter
		Parameter newParameter = parameterAccessor.get(new ObjectId(id));
		newParameter.setId(new ObjectId());
		return save(newParameter, sourceParameter);
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
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-read")
	public Parameter get(@PathParam("id") String id) {
		Parameter parameter = parameterAccessor.get(new ObjectId(id));
		return maskProtectedValue(parameter);
	}
	
	public static Parameter maskProtectedValue(Parameter parameter) {
		if(parameter != null && isProtected(parameter) &&
				!ParameterManager.RESET_VALUE.equals(parameter.getValue())) {
			parameter.setValue(PROTECTED_VALUE);				
		}
		return parameter;
	}
	
	protected List<Parameter> maskProtectedValues(Stream<Parameter> stream) {
		return stream.map(p->maskProtectedValue(p)).collect(Collectors.toList());
	}

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-read")
	public Parameter get(Map<String,String> attributes) {
		return maskProtectedValue(parameterAccessor.findByAttributes(attributes));
	}

	@POST
	@Path("/find")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-read")
	public List<Parameter> findMany(Map<String, String> attributes) {
		return maskProtectedValues(StreamSupport.stream(parameterAccessor.findManyByAttributes(attributes), false));
	}

	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="param-read")
	public List<Parameter> getAll(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		List<Parameter> range;
		if(skip != null && limit != null) {
			range = parameterAccessor.getRange(skip, limit);
		} else {
			range = getAll(0, 1000);
		}
		return maskProtectedValues(range.stream());
	}
}
