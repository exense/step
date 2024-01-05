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

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionLocator;
import step.artefacts.handlers.SelectorHelper;
import step.controller.services.entities.AbstractEntityServices;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.ControllerServiceException;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.entities.EntityManager;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.framework.server.Session;
import step.framework.server.security.Secured;
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
import step.planbuilder.FunctionArtefacts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbtractFunctionServices extends AbstractEntityServices<Function> {

	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	protected FunctionAccessor functionAccessor;
	protected FunctionManager functionManager;
	
	protected FunctionExecutionService functionExecutionService;
	
	protected SelectorHelper selectorHelper;
	protected FunctionLocator functionLocator;
	protected ObjectPredicateFactory objectPredicateFactory;

	public AbtractFunctionServices() {
		super(EntityManager.functions);
	}

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

	@Operation(operationId = "getAll{Entity}s")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public List<Function> getAllFunctions(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return functionAccessor.getRange(skip, limit);
		} else {
			return getAllFunctions(0, 1000);
		}
	}

	@Operation(operationId = "search{Entity}")
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Function searchFunction(Map<String,String> attributes) {
		return functionManager.getFunctionByAttributes(attributes);
	}

	@Operation(operationId = "lookupCall{Entity}")
	@POST
	@Path("/lookup")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Function lookupCallFunction(CallFunction callFunction) {
		Function function = null;
		try {
			ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
			function = functionLocator.getFunction(callFunction, objectPredicate, null);
		} catch (RuntimeException e) {}
		return function;
	}

	@Operation(operationId = "get{Entity}Editor")
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("/{id}/editor")
	@Secured(right="{entity}-read")
	public String getFunctionEditor(@PathParam("id") String functionId) {
		Function function = functionManager.getFunctionById(functionId);
		FunctionEditor editor = getContext().get(FunctionEditorRegistry.class).getFunctionEditor(function);
		if(editor!=null) {
			return editor.getEditorPath(function);
		} else {
			return null;
		}
	}

	@Override
	public Function save(Function function) {
		try {
			return functionManager.saveFunction(function);
		} catch (SetupFunctionException | FunctionTypeException e) {
			throw new ControllerServiceException(e.getMessage());
		}
	}

	@Override
	public Function clone(String id) {
		try {
			return functionManager.copyFunction(id);
		} catch (FunctionTypeException e) {
			throw new ControllerServiceException(e.getMessage());
		}
	}

	@Override
	public void delete(String functionId) {
		try {
			functionManager.deleteFunction(functionId);
		} catch (FunctionTypeException e) {
			throw new ControllerServiceException(e.getMessage());
		}
	}

	@Operation(operationId = "new{Entity}TypeConf")
	@GET
	@Path("/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="{entity}-read")
	public Function newFunctionTypeConf(@PathParam("id") String type) {
		Function newFunction = functionManager.newFunction(type);
		newFunction.setAttributes(new HashMap<>());
		newFunction.getAttributes().put(AbstractOrganizableObject.NAME, "");
		newFunction.setSchema(Json.createObjectBuilder().build());
		getObjectEnricher().accept(newFunction);
		return newFunction;
	}

	@Operation(operationId = "get{Entity}TokenHandle")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/select")
	@Secured(right="{entity}-execute")
	public TokenWrapper getTokenHandle(GetTokenHandleParameter parameter, @Context HttpServletRequest req) throws FunctionExecutionServiceException {
		Session<User> session = getSession();
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

	@Operation(operationId = "return{Entity}TokenHandle")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/return")
	@Secured(right="{entity}-execute")
	public void returnTokenHandle(@PathParam("id") String tokenId) throws FunctionExecutionServiceException {
		functionExecutionService.returnTokenHandle(tokenId);
	}

	@Operation(operationId = "call{Entity}ById")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/execute/{functionId}")
	@Secured(right="{entity}-execute")
	public Output<JsonObject> callFunctionById(@PathParam("id") String tokenId, @PathParam("functionId") String functionId, FunctionInput<JsonObject> input) {
		Function function = functionManager.getFunctionById(functionId);
		return functionExecutionService.callFunction(tokenId, function, input, JsonObject.class);			
	}

	@Operation(operationId = "call{Entity}ByAttributes")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/executor/tokens/{id}/execute")
	@Secured(right="{entity}-execute")
	public Output<JsonObject> callFunctionByAttributes(@PathParam("id") String tokenId, FunctionInput<JsonObject> input, @Context UriInfo uriInfo) {
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
