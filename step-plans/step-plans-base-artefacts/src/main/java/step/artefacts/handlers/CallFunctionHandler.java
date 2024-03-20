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
package step.artefacts.handlers;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParsingException;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.functions.*;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.common.managedoperations.OperationManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.docker.DockerRegistryConfiguration;
import step.core.docker.DockerRegistryConfigurationAccessor;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.execution.ExecutionContextWrapper;
import step.core.json.JsonProviderCache;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.core.plans.Plan;
import step.core.plugins.ExecutionCallbacks;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariablesManager;
import step.datapool.DataSetHandle;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.tokenpool.Interest;
import step.plugins.functions.types.CompositeFunction;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	private static final String KEYWORD_OUTPUT_LEGACY_FORMAT = "keywords.output.legacy";
	public static final String OPERATION_KEYWORD_CALL = "Keyword Call";

	protected FunctionExecutionService functionExecutionService;
	protected FunctionAccessor functionAccessor;
	
	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;

	private TokenSelectionCriteriaMapBuilder tokenSelectionCriteriaMapBuilder;
	protected FunctionLocator functionLocator;

	protected boolean useLegacyOutput;
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		FunctionTypeRegistry functionTypeRegistry = context.require(FunctionTypeRegistry.class);
		functionAccessor = context.require(FunctionAccessor.class);
		functionExecutionService = context.require(FunctionExecutionService.class);
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		this.tokenSelectionCriteriaMapBuilder = new TokenSelectionCriteriaMapBuilder(functionTypeRegistry, dynamicJsonObjectResolver);
		this.functionLocator = new FunctionLocator(functionAccessor, new SelectorHelper(dynamicJsonObjectResolver));
		this.useLegacyOutput = context.getConfiguration().getPropertyAsBoolean(KEYWORD_OUTPUT_LEGACY_FORMAT, false);
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {
		// TODO this is a draft
		try {
			Function function = getFunction(testArtefact);
			if(function instanceof CompositeFunction) {
				Plan plan = ((CompositeFunction) function).getPlan();
				delegateCreateReportSkeleton(plan.getRoot(), parentNode);
			} else {
				FunctionGroupContext functionGroupContext = getFunctionGroupContext();
				boolean closeFunctionGroupSessionAfterExecution = (functionGroupContext == null);

				FunctionGroupSession functionGroupSession = getOrCreateFunctionGroupSession(functionGroupContext);
				try {
					selectToken(parentNode, testArtefact, function, functionGroupContext, functionGroupSession);
				} finally {
					if(closeFunctionGroupSessionAfterExecution) {
						functionGroupSession.close();
					}
				}
			}
		} catch (Exception e) {
			// TODO improve handling
			e.printStackTrace();
			logger.debug("No able to find function during skeleton creation phase", e);
		}
	}

	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) throws Exception {
		String argumentStr = testArtefact.getArgument().get();
		node.setInput(argumentStr);
		
		Function function = getFunction(testArtefact);
		
		ExecutionCallbacks executionCallbacks = context.getExecutionCallbacks();
		executionCallbacks.beforeFunctionExecution(context, node, function);
		
		node.setFunctionId(function.getId().toString());
		node.setFunctionAttributes(function.getAttributes());

		String name = node.getName();
		// Name the report node after the keyword if it's not already the case
		String functionName = function.getAttribute(AbstractOrganizableObject.NAME);
		if(name.equals(CallFunction.ARTEFACT_NAME) && functionName != null) {
			node.setName(functionName);
		}
		
		FunctionInput<JsonObject> input = buildInput(argumentStr);
		node.setInput(input.getPayload().toString());
		
		validateInput(input, function);

		Output<JsonObject> output;
		if(!context.isSimulation()) {
			FunctionGroupContext functionGroupContext = getFunctionGroupContext();
			boolean closeFunctionGroupSessionAfterExecution = (functionGroupContext == null);
			FunctionGroupSession functionGroupSession = getOrCreateFunctionGroupSession(functionGroupContext);
			TokenWrapper token = selectToken(node, testArtefact, function, functionGroupContext, functionGroupSession);

			try {
				Token gridToken = token.getToken();
				if(gridToken.isLocal()) {
					TokenReservationSession session = (TokenReservationSession) gridToken.getAttachedObject(TokenWrapper.TOKEN_RESERVATION_SESSION);
					session.put(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY, new ExecutionContextWrapper(context));
				}
				
				node.setAgentUrl(token.getAgent().getAgentUrl());
				node.setTokenId(token.getID());
				
				OperationManager.getInstance().enter(OPERATION_KEYWORD_CALL, new Object[]{function.getAttributes(), token.getToken(), token.getAgent()},
						node.getId().toString());

				// Add the docker image if present within the session
				if(functionGroupContext != null) {
					functionGroupContext.dockerImage.ifPresent(image -> {
						DockerRegistryConfigurationAccessor dockerRegistryConfigurationAccessor = context.require(DockerRegistryConfigurationAccessor.class);
						DockerRegistryConfiguration dockerRegistryConfiguration = dockerRegistryConfigurationAccessor
								.stream()
								.filter(registryConfiguration -> {
											try {
												return image.contains(new URL(registryConfiguration.getUrl()).getAuthority());
											} catch (MalformedURLException e) {
												throw new RuntimeException(e);
											}
										})
								.findFirst().orElseThrow(()	-> new NoSuchElementException(String.format("No docker registry matching image path %s found, it must first be created", image)));
						Map<String, String> inputProperties = input.getProperties();
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_DOCKER_IMAGE, image);
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_CONTAINER_USER, functionGroupContext.containerUser.orElseThrow(() -> new NoSuchElementException("No container user has been specified, this is mandatory")));
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_CONTAINER_CMD, functionGroupContext.containerCommand.orElse(""));
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_DOCKER_REGISTRY_URL, dockerRegistryConfiguration.getUrl());
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_DOCKER_REGISTRY_USERNAME, dockerRegistryConfiguration.getUsername());
						inputProperties.put(FunctionExecutionServiceImpl.INPUT_PROPERTY_DOCKER_REGISTRY_PASSWORD, dockerRegistryConfiguration.getPassword());
						input.setProperties(inputProperties);
					});
				}
				try {
					output = functionExecutionService.callFunction(token.getID(), function, input, JsonObject.class, context);
				} finally {
					OperationManager.getInstance().exit();
				}
				executionCallbacks.afterFunctionExecution(context, node, function, output);
				
				Error error = output.getError();
				if(error!=null) {
					node.setError(error);
					node.setStatus(error.getType()==ErrorType.TECHNICAL?ReportNodeStatus.TECHNICAL_ERROR:ReportNodeStatus.FAILED);
				} else {
					node.setStatus(ReportNodeStatus.PASSED);
				}
	
				if(output.getPayload() != null) {
					Object outputPayload = (useLegacyOutput) ? output.getPayload() : new UserFriendlyJsonObject(output.getPayload());
					context.getVariablesManager().putVariable(node, "output", outputPayload);
					node.setOutput(output.getPayload().toString());
					node.setOutputObject(output.getPayload());
					ReportNode parentNode = context.getReportNodeCache().get(node.getParentID());
					if(parentNode!=null) {
						context.getVariablesManager().putVariable(parentNode, "previous", outputPayload);
					}
				}
				
				if(output.getAttachments()!=null) {
					for(Attachment a:output.getAttachments()) {
						AttachmentMeta attachmentMeta;
						try {
							attachmentMeta = reportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
							node.addAttachment(attachmentMeta);					
						} catch (AttachmentQuotaException e) {
							// attachment has been skipped. Nothing else to do here
						}
					}
				}
				if(output.getMeasures()!=null) {
					node.setMeasures(output.getMeasures());
				}
				
				String drainOutputValue = testArtefact.getResultMap().get();
				drainOutput(drainOutputValue, output);
			} finally {
				if(closeFunctionGroupSessionAfterExecution) {
					functionGroupSession.releaseTokens(true);
				}
	
				callChildrenArtefacts(node, testArtefact);
			}
		} else {
			output = new Output<>();
			output.setPayload(JsonProviderCache.createObjectBuilder().build());
			node.setOutputObject(output.getPayload());
			node.setOutput(output.getPayload().toString());
			node.setStatus(ReportNodeStatus.PASSED);
		}
	}

	private FunctionGroupContext getFunctionGroupContext() {
		return (FunctionGroupContext) context.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
	}

	private FunctionGroupSession getOrCreateFunctionGroupSession(FunctionGroupContext functionGroupContext) {
		FunctionGroupSession functionGroupSession;
		if (functionGroupContext == null) {
			TokenNumberCalculationContext calculationContext = AutoscalerExecutionPlugin.getTokenNumberCalculationContext(context);
			FunctionExecutionService functionExecutionService = calculationContext.getFunctionExecutionServiceForTokenRequirementCalculation();
			functionGroupSession = new FunctionGroupSession(functionExecutionService);
		} else {
			functionGroupSession = functionGroupContext.getSession();
		}
		return functionGroupSession;
	}

	private TokenWrapper selectToken(CallFunctionReportNode node, CallFunction testArtefact, Function function, FunctionGroupContext functionGroupContext, FunctionGroupSession functionGroupSession) throws FunctionExecutionServiceException {
		CallFunctionTokenWrapperOwner tokenWrapperOwner = new CallFunctionTokenWrapperOwner(node.getId().toString(), context.getExecutionId(), context.getExecutionParameters().getDescription());
		boolean localTokenRequired = tokenSelectionCriteriaMapBuilder.isLocalTokenRequired(testArtefact, function);
		TokenWrapper token;
		if(localTokenRequired) {
			token = functionGroupSession.getLocalToken();
		} else {
			Map<String, Interest> tokenSelectionCriteria = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(testArtefact, function, functionGroupContext, getBindings());
			token = functionGroupSession.getRemoteToken(tokenSelectionCriteria, tokenWrapperOwner);
		}
		return token;
	}

	private void validateInput(FunctionInput<JsonObject> input, Function function) {
		if(context.getConfiguration().getPropertyAsBoolean("enforceschemas", false)){
			JsonSchemaValidator.validate(function.getSchema().toString(), input.getPayload().toString());
		}
	}

	private Function getFunction(CallFunction testArtefact) {
		return functionLocator.getFunction(testArtefact, context.getObjectPredicate(),
				ExecutionContextBindings.get(context));
	}

	@SuppressWarnings("unchecked")
	private void drainOutput(String drainOutputValue, Output<JsonObject> output) {
		if(drainOutputValue!=null&&drainOutputValue.trim().length()>0) {
			JsonObject resultJson = output.getPayload();
			if(resultJson!=null) {
				Object var = context.getVariablesManager().getVariable(drainOutputValue);
				if(var instanceof Map) {
					Map<String, String> resultMap = jsonToMap(resultJson);
					((Map<String, String>)var).putAll(resultMap);
				} else if(var instanceof DataSetHandle) {
					DataSetHandle dataSetHandle = (DataSetHandle) var;
					Map<String, String> resultMap = jsonToMap(resultJson);
					for(String key:resultJson.keySet()) {
						JsonValue jsonValue = resultJson.get(key);
						if(jsonValue instanceof JsonArray) {
							JsonArray array = (JsonArray) jsonValue;
							array.forEach(value-> {
								if(value.getValueType().equals(ValueType.OBJECT)) {
									Map<String, String> rowAsMap = jsonToMap((JsonObject) value);
									dataSetHandle.addRow(rowAsMap);
								}
							});
						}
					}
					if(!resultMap.isEmpty()) {
						dataSetHandle.addRow(resultMap);						
					}
				} else {
					throw new RuntimeException("The variable '"+drainOutputValue+"' is neither a Map nor a DataSet handle");
				}					
			}
		}
	}

	private Map<String, String> jsonToMap(JsonObject jsonOutput) {
		Map<String, String> resultMap = new LinkedHashMap<>();
		for(String key:jsonOutput.keySet()) {
			JsonValue value = jsonOutput.get(key);
			if(value.getValueType() == ValueType.STRING) {
				resultMap.put(key, jsonOutput.getString(key));
			} else if (!value.getValueType().equals(ValueType.OBJECT)&&!value.getValueType().equals(ValueType.ARRAY)) {
				resultMap.put(key, jsonOutput.getString(key).toString());
			}
		}
		return resultMap;
	}

	protected void callChildrenArtefacts(CallFunctionReportNode node, CallFunction testArtefact) {
		if(testArtefact.getChildren()!=null&&testArtefact.getChildren().size()>0) {
			VariablesManager variableManager = context.getVariablesManager();
			variableManager.putVariable(node, "callReport", node);
			
//			node.getOutputObject().forEach((k,v)->{
//				variableManager.putVariable(node, k, v.toString());
//			});
			
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.execute_(node, testArtefact, true);				
		}
	}
	
	private FunctionInput<JsonObject> buildInput(String argumentStr) {
		JsonObject argument = parseAndResolveJson(argumentStr);
		
		Map<String, String> properties = new HashMap<>();
		context.getVariablesManager().getAllVariables().forEach((key,value)->properties.put(key, value!=null?value.toString():""));
		properties.put(AbstractFunctionHandler.PARENTREPORTID_KEY, context.getCurrentReportNode().getId().toString());
		
		FunctionInput<JsonObject> input = new FunctionInput<>();
		input.setPayload(argument);
		input.setProperties(properties);
		return input;
	}

	private JsonObject parseAndResolveJson(String functionStr) {
		JsonObject query;
		try {
			if(functionStr!=null&&functionStr.trim().length()>0) {
				query = JsonProviderCache.createReader(new StringReader(functionStr)).readObject();
			} else {
				query = JsonProviderCache.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing argument (input): string was '"+functionStr+"'",e);
		}
		return dynamicJsonObjectResolver.evaluate(query, getBindings());
	}


	@Override
	public CallFunctionReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new CallFunctionReportNode();
	}
}
