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
package step.controller.grid.services;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Session;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.manager.FunctionManager;
import step.functions.services.GetTokenHandleParameter;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.TokenWrapper;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbtractFunctionServices extends AbstractServices {

	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	protected FunctionAccessor functionAccessor;
	protected FunctionManager functionManager;
	
	protected FunctionExecutionService functionExecutionService;
	
	protected SelectorHelper selectorHelper;
	protected FunctionLocator functionLocator;
	protected ObjectPredicateFactory objectPredicateFactory;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(getContext().getResourceManager());
		functionAccessor = getContext().get(FunctionAccessor.class);
		functionManager = getContext().get(FunctionManager.class);
		functionExecutionService = getContext().get(FunctionExecutionService.class);
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		functionLocator = new FunctionLocator(functionAccessor, selectorHelper);
		objectPredicateFactory = getContext().get(ObjectPredicateFactory.class);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public List<Function> getAll(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return functionAccessor.getRange(skip, limit);
		} else {
			return getAll(0, 1000);
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public Function get(@PathParam("id") String functionId) {
		return functionManager.getFunctionById(functionId);
	}
	
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public Function get(Map<String,String> attributes) {
		return functionManager.getFunctionByAttributes(attributes);
	}
	
	@POST
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public List<Function> findMany(Map<String,String> attributes) {
		return StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
	}
	
	@POST
	@Path("/lookup")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public Function lookupCallFunction(CallFunction callFunction) {
		Function function = null;
		try {
			ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
			function = functionLocator.getFunction(callFunction, objectPredicate, null);
		} catch (RuntimeException e) {}
		return function;
	}
	
	@GET
	@Path("/{id}/editor")
	@Secured(right="read")
	public String getFunctionEditor(@PathParam("id") String functionId) {
		Function function = functionManager.getFunctionById(functionId);
		FunctionEditor editor = getContext().get(FunctionEditorRegistry.class).getFunctionEditor(function);
		if(editor!=null) {
			return editor.getEditorPath(function);
		} else {
			return null;
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="write")
	public Function save(Function function) throws SetupFunctionException, FunctionTypeException {
		return functionManager.saveFunction(function);
	}
	
	@POST
	@Path("/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="write")
	public void copyFunction(@PathParam("id") String id) throws FunctionTypeException {		
		functionManager.copyFunction(id);
	}
	
	@DELETE
	@Path("/{id}")
	@Secured(right="delete")
	public void delete(@PathParam("id") String functionId) throws FunctionTypeException {
		functionManager.deleteFunction(functionId);
	}
	
	@GET
	@Path("/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="read")
	public Function newFunctionTypeConf(@PathParam("id") String type) {
		Function newFunction = functionManager.newFunction(type);
		newFunction.setAttributes(new HashMap<>());
		newFunction.getAttributes().put(AbstractOrganizableObject.NAME, "");
		newFunction.setSchema(Json.createObjectBuilder().build());
		return newFunction;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/select")
	@Secured(right="execute")
	public TokenWrapper getTokenHandle(GetTokenHandleParameter parameter, @Context HttpServletRequest req) throws FunctionExecutionServiceException {
		Session session = getSession();
		if(!parameter.isLocal()) {
			FunctionServiceTokenWrapperOwner tokenWrapperOwner = new FunctionServiceTokenWrapperOwner();
			tokenWrapperOwner.setUsername(session.getUser().getUsername());
			tokenWrapperOwner.setIpAddress(req.getRemoteAddr());
			tokenWrapperOwner.setDescription(parameter.getReservationDescription());
			return functionExecutionService.getTokenHandle(parameter.getAttributes(), parameter.getInterests(), parameter.isCreateSession(), tokenWrapperOwner);
		} else {
			return functionExecutionService.getLocalTokenHandle();
		}
		
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/return")
	@Secured(right="execute")
	public void returnTokenHandle(@PathParam("id") String tokenId) throws FunctionExecutionServiceException {
		functionExecutionService.returnTokenHandle(tokenId);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/execute/{functionId}")
	@Secured(right="execute")
	public Output<JsonObject> callFunction(@PathParam("id") String tokenId, @PathParam("functionId") String functionId, FunctionInput<JsonObject> input) {
		Function function = functionManager.getFunctionById(functionId);
		return functionExecutionService.callFunction(tokenId, function, input, JsonObject.class);			
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/execute")
	@Secured(right="execute")
	public Output<JsonObject> callFunction(@PathParam("id") String tokenId, FunctionInput<JsonObject> input, @Context UriInfo uriInfo) {
		Map<String,String> functionAttributes = new HashMap<>();
		uriInfo.getQueryParameters().entrySet().forEach(e->{
			functionAttributes.put(e.getKey(), e.getValue().get(0));
		});
		Function function = functionAccessor.findByAttributes(functionAttributes);
		if(function == null) {
			throw new RuntimeException("No function found matching the attributes "+functionAttributes);
		}
		return functionExecutionService.callFunction(tokenId, function, input, JsonObject.class);			
	}
}
