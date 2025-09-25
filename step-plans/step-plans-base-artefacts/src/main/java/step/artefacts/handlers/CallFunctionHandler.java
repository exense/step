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
import org.bson.types.ObjectId;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.functions.FunctionGroupSession;
import step.artefacts.handlers.functions.TokenSelectionCriteriaMapBuilder;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.attachments.StreamingAttachmentMeta;
import step.common.managedoperations.OperationManager;
import step.constants.LiveReportingConstants;
import step.constants.StreamingConstants;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.SequentialArtefactScheduler;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.resolvedplan.ResolvedChildren;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.execution.ExecutionContextWrapper;
import step.core.execution.OperationMode;
import step.core.json.JsonProviderCache;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.objectenricher.ObjectEnricher;
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
import step.livereporting.LiveReportingContext;
import step.livereporting.LiveReportingPlugin;
import step.plugins.functions.types.CompositeFunction;
import step.streaming.common.*;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.core.agents.provisioning.AgentPoolConstants.TOKEN_ATTRIBUTE_PARTITION;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	private static final String KEYWORD_OUTPUT_LEGACY_FORMAT = "keywords.output.legacy";
	public static final String OPERATION_KEYWORD_CALL = "Keyword Call";

	protected FunctionExecutionService functionExecutionService;
	protected FunctionAccessor functionAccessor;
	protected ReportNodeAccessor reportNodeAccessor;

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
		reportNodeAccessor = context.getReportNodeAccessor();
		functionExecutionService = context.require(FunctionExecutionService.class);
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		this.tokenSelectionCriteriaMapBuilder = new TokenSelectionCriteriaMapBuilder(functionTypeRegistry, dynamicJsonObjectResolver);
		this.functionLocator = new FunctionLocator(functionAccessor, new SelectorHelper(dynamicJsonObjectResolver));
		this.useLegacyOutput = context.getConfiguration().getPropertyAsBoolean(KEYWORD_OUTPUT_LEGACY_FORMAT, false);
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {
		try {
			Function function = getFunction(testArtefact);
			if (function instanceof CompositeFunction) {
				Plan plan = ((CompositeFunction) function).getPlan();
				delegateCreateReportSkeleton(plan.getRoot(), parentNode);
			} else {
				FunctionGroupContext functionGroupContext = getFunctionGroupContext();
				boolean closeFunctionGroupSessionAfterExecution = (functionGroupContext == null);

				// Inject the mocked function execution service of the token forecasting context instead of the function execution service of the context
				FunctionExecutionService functionExecutionService = getTokenForecastingContext(context).getFunctionExecutionServiceForTokenForecasting();
				FunctionGroupSession functionGroupSession = getOrCreateFunctionGroupSession(functionExecutionService, functionGroupContext);
				try {
					// Do not force the local token selection in order to simulate a real token selection
					selectToken(parentNode, testArtefact, function, functionGroupContext, functionGroupSession, false);
				} finally {
					if (closeFunctionGroupSessionAfterExecution) {
						functionGroupSession.close();
					}
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring error during skeleton phase", e);
			}
		}
	}

	@Override
	public boolean requiresToPushArtefactPathOnResolution() {
		return true;
	}


	@Override
	protected List<ResolvedChildren> resolveChildrenArtefactBySource_(CallFunction artefactNode, String currentArtefactPath) {
		List<ResolvedChildren> results = new ArrayList<>();
		try {
			dynamicBeanResolver.evaluate(artefactNode, getBindings());
			Function function = getFunction(artefactNode);
			if(function instanceof CompositeFunction) {
					AbstractArtefact root = ((CompositeFunction) function).getPlan().getRoot();
					results.add(new ResolvedChildren(ParentSource.SUB_PLAN, List.of(root), currentArtefactPath));
			}
		} catch (NoSuchElementException e) {
			String message = "Unable to resolve called composite keyword in plan";
			if (logger.isDebugEnabled()) {
				logger.debug(message, e);
			} else {
				logger.warn(message);
			}
		} catch (RuntimeException e) {
			//groovy selection attributes cannot be evaluated at this stage, ignoring
			if (logger.isTraceEnabled()) {
				logger.trace("Unable to resolve the function referenced by this callFunction '{}' artefact at this stage.", artefactNode.getAttribute(AbstractOrganizableObject.NAME), e);
			}
		}
		results.add(new ResolvedChildren(ParentSource.MAIN, artefactNode.getChildren(), currentArtefactPath));
		return results;
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
		Map<String, StreamingAttachmentMeta> streamingAttachments = new ConcurrentHashMap<>();

		if(!context.isSimulation()) {
			FunctionGroupContext functionGroupContext = getFunctionGroupContext();
			boolean closeFunctionGroupSessionAfterExecution = (functionGroupContext == null);
			FunctionGroupSession functionGroupSession = getOrCreateFunctionGroupSession(functionExecutionService, functionGroupContext);

			// Force local token selection for local plan executions
			boolean forceLocalToken =  context.getOperationMode() == OperationMode.LOCAL;
			TokenWrapper token = selectToken(node, testArtefact, function, functionGroupContext, functionGroupSession, forceLocalToken);

			StreamingResourceUploadContext uploadContext = null;
			String liveMeasureContextHandle = null;

			try {
				String agentUrl = token.getAgent().getAgentUrl();
				node.setAgentUrl(agentUrl);
				node.setTokenId(token.getID());

				Token gridToken = token.getToken();



				/* Support for streaming uploads produced during this call. We create and register a new context,
				provide the necessary information for the upload provider, and set up a listener for the context,
				so we can populate the attachment metadata in realtime and attach it to the report node.
				*/
				// FIXME: SED-4192 (Step 30+) This will currently only work in a full Step server, not for local AP executions, Unit Tests etc.
                StreamingResourceUploadContexts uploadContexts = context.get(StreamingResourceUploadContexts.class);
				if (uploadContexts != null) {
					uploadContext = new StreamingResourceUploadContext();
					uploadContexts.registerContext(uploadContext);
					uploadContext.getAttributes().put(StreamingConstants.AttributeNames.RESOURCE_EXECUTION_ID, context.getExecutionId());
					uploadContext.getAttributes().put(StreamingConstants.AttributeNames.VARIABLES_MANAGER, context.getVariablesManager());
					uploadContext.getAttributes().put(StreamingConstants.AttributeNames.REPORT_NODE, node);
					ObjectEnricher enricher = context.getObjectEnricher();
					if (enricher != null) {
						uploadContext.getAttributes().put(StreamingConstants.AttributeNames.ACCESS_CONTROL_ENRICHER, enricher);
					}

					input.getProperties().put(StreamingConstants.AttributeNames.WEBSOCKET_BASE_URL, (String) context.get(StreamingConstants.AttributeNames.WEBSOCKET_BASE_URL));
					input.getProperties().put(StreamingConstants.AttributeNames.WEBSOCKET_UPLOAD_PATH, (String) context.get(StreamingConstants.AttributeNames.WEBSOCKET_UPLOAD_PATH));
					input.getProperties().put(StreamingResourceUploadContext.PARAMETER_NAME, uploadContext.contextId);


					uploadContexts.registerListener(uploadContext.contextId, new StreamingResourceUploadContextListener() {

						@Override
						public void onResourceCreationRefused(StreamingResourceMetadata metadata, String reasonPhrase) {
							node.getAttachments().add(new SkippedAttachmentMeta(metadata.getFilename(), metadata.getMimeType(), reasonPhrase));
							reportNodeAccessor.save(node);
						}

						@Override
						public void onResourceCreated(String resourceId, StreamingResourceMetadata metadata) {
							// This will create an attachment with its immutable properties, but it will not yet "publish" it to the reportNode or set its status etc.
							streamingAttachments.put(resourceId, new StreamingAttachmentMeta(new ObjectId(resourceId), metadata.getFilename(), metadata.getMimeType()));
						}

						@Override
						public void onResourceStatusChanged(String resourceId, StreamingResourceStatus status) {
							// Here's where we update the attachment status etc.
							StreamingAttachmentMeta attachment = streamingAttachments.get(resourceId);
							if (attachment != null) {
								// initially, there is no status set (see above)
								boolean isFirstUpdate = attachment.getStatus() == null;
								attachment.setCurrentSize(status.getCurrentSize());
								attachment.setCurrentNumberOfLines(status.getNumberOfLines());
								attachment.setStatus(StreamingAttachmentMeta.Status.valueOf(status.getTransferStatus().name()));
								if (isFirstUpdate) {
									// this ensures that attachments are added to the node exactly once, and with meaningful initial data
									node.getAttachments().add(attachment);
								}
								reportNodeAccessor.save(node);
							} else {
								logger.warn("Unexpected: Unable to find attachment for resource '{}'", resourceId);
							}
						}
					});
				}

				LiveReportingContext liveReportingContext = LiveReportingPlugin.getLiveReportingContext(context);
				if (liveReportingContext != null) {
					// set up the plumbing to let the handler know where to forward measures
					String url = liveReportingContext.getReportingUrl();
					input.getProperties().put(LiveReportingConstants.AttributeNames.LIVEREPORTING_CONTEXT_URL, url);
				}

				if(gridToken.isLocal()) {
					TokenReservationSession session = (TokenReservationSession) gridToken.getAttachedObject(TokenWrapper.TOKEN_RESERVATION_SESSION);
					session.put(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY, new ExecutionContextWrapper(context));
					session.put(AbstractFunctionHandler.ARTEFACT_PATH, currentArtefactPath());
				} else {
					// only report non-local (i.e. actual agent) URLs
					context.addAgentUrl(agentUrl);
				}

				OperationManager.getInstance().enter(OPERATION_KEYWORD_CALL, new Object[]{function.getAttributes(), token.getToken(), token.getAgent()},
						node.getId().toString(), node.getArtefactHash());

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
						AttachmentMeta attachmentMeta = reportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName(), a.getMimeType());
						node.addAttachment(attachmentMeta);
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
				if (uploadContext != null) {
					context.require(StreamingResourceUploadContexts.class).unregisterContext(uploadContext);
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

	private FunctionGroupSession getOrCreateFunctionGroupSession(FunctionExecutionService functionExecutionService, FunctionGroupContext functionGroupContext) {
		FunctionGroupSession functionGroupSession;
		if (functionGroupContext == null) {
			functionGroupSession = new FunctionGroupSession(functionExecutionService);
		} else {
			functionGroupSession = functionGroupContext.getSession();
		}
		return functionGroupSession;
	}

	private TokenWrapper selectToken(CallFunctionReportNode node, CallFunction testArtefact, Function function, FunctionGroupContext functionGroupContext, FunctionGroupSession functionGroupSession, boolean localToken) throws FunctionExecutionServiceException {
		CallFunctionTokenWrapperOwner tokenWrapperOwner = new CallFunctionTokenWrapperOwner(node.getId().toString(), context.getExecutionId(), context.getExecutionParameters().getDescription());
		boolean localTokenRequired = localToken || tokenSelectionCriteriaMapBuilder.isLocalTokenRequired(testArtefact, function);
		TokenWrapper token;
		if(localTokenRequired) {
			token = functionGroupSession.getLocalToken();
		} else {
			Map<String, Interest> tokenSelectionCriteria = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(testArtefact, function, functionGroupContext, getBindings());
			boolean skipAutoProvisioning = tokenSelectionCriteriaMapBuilder.shouldSkipAutoProvisioning(testArtefact, function, functionGroupContext, getBindings());
			token = functionGroupSession.getRemoteToken(getOwnAttributesForTokenSelection(), tokenSelectionCriteria, tokenWrapperOwner, (functionGroupContext != null), skipAutoProvisioning);
		}
		return token;
	}

	/**
	 * @return the map of attributes that will be presented for the token selection to the agent token.
	 * These attributes will be used to match the right token if the agent token defines criteria for the selector (token pretender)
	 */
	private Map<String, String> getOwnAttributesForTokenSelection() {
		String executionId = context.getExecutionId();
		return Map.of(TOKEN_ATTRIBUTE_PARTITION, executionId);
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
		if (drainOutputValue != null && drainOutputValue.trim().length() > 0) {
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
		if (testArtefact.getChildren() != null && testArtefact.getChildren().size() > 0) {
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
			if (functionStr != null && functionStr.trim().length() > 0) {
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
