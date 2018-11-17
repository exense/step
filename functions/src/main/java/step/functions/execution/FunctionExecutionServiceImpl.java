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
package step.functions.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.functions.Function;
import step.functions.Input;
import step.functions.Output;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.client.GridClient;
import step.grid.client.GridClientImpl.AgentCallTimeoutException;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.client.GridClientImpl.AgentSideException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.AgentError;
import step.grid.io.AgentErrorCode;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionExecutionServiceImpl implements FunctionExecutionService {

	private final GridClient gridClient;
	
	private final FunctionAccessor functionAccessor;
	
	private final FunctionTypeRegistry functionTypeRegistry;
	
	private final DynamicBeanResolver dynamicBeanResolver;
	
	private final FileVersionId functionHandlerPackage;
	
	private final ObjectMapper mapper;
	
	public FunctionExecutionServiceImpl(GridClient gridClient, FunctionAccessor functionAccessor,
			FunctionTypeRegistry functionTypeRegistry, DynamicBeanResolver dynamicBeanResolver) {
		super();
		this.gridClient = gridClient;
		this.functionAccessor = functionAccessor;
		this.functionTypeRegistry = functionTypeRegistry;
		this.dynamicBeanResolver = dynamicBeanResolver;
	
		File functionHandlerJar = ResourceExtractor.extractResource(getClass().getClassLoader(), "functions-api.jar");
		long version = FileHelper.getLastModificationDateRecursive(functionHandlerJar);
		
		functionHandlerPackage = new FileVersionId(gridClient.registerFile(functionHandlerJar), version);
		
		mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
	}

	private static final Logger logger = LoggerFactory.getLogger(FunctionExecutionServiceImpl.class);
	
	@Override
	public TokenWrapper getLocalTokenHandle() {
		return gridClient.getLocalTokenHandle();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws FunctionExecutionServiceException {
		try {
			return gridClient.getTokenHandle(attributes, interests, createSession);
		} catch (AgentCallTimeoutException e) {
			throw new FunctionExecutionServiceException("Timeout after "+e.getCallTimeout()+"ms while reserving the agent token. You can increase the call timeout by setting 'grid.client.token.reserve.timeout.ms' in step.properties",e );
		} catch (AgentSideException e) {
			throw new FunctionExecutionServiceException("Unexepected error on the agent side while reserving the agent token: "+e.getMessage(),e);
		} catch (AgentCommunicationException e) {
			throw new FunctionExecutionServiceException("Communication error between the controller and the agent while reserving the agent token",e);
		} 
	}

	@Override
	public void returnTokenHandle(TokenWrapper adapterToken) throws FunctionExecutionServiceException {
		try {
			gridClient.returnTokenHandle(adapterToken);
		} catch (AgentCallTimeoutException e) {
			throw new FunctionExecutionServiceException("Timeout after "+e.getCallTimeout()+"ms while releasing the agent token. You can increase the call timeout by setting 'grid.client.token.release.timeout.ms' in step.properties",e );
		} catch (AgentSideException e) {
			throw new FunctionExecutionServiceException("Unexepected error on the agent side while releasing the agent token: "+e.getMessage(),e);
		} catch (AgentCommunicationException e) {
			throw new FunctionExecutionServiceException("Communication error between the controller and the agent while releasing the agent token",e);
		} 
	}
	
	@Override
	public <IN,OUT> Output<OUT> callFunction(TokenWrapper tokenHandle, Map<String,String> functionAttributes, Input<IN> input, Class<OUT> outputClass) {	
		Function function = functionAccessor.findByAttributes(functionAttributes);
		return callFunction(tokenHandle, function.getId().toString(), input, outputClass);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <IN,OUT> Output<OUT>  callFunction(TokenWrapper tokenHandle, String functionId, Input<IN> input, Class<OUT> outputClass) {	
		Function function = functionAccessor.get(new ObjectId(functionId));
		
		input.setFunction(function.getAttributes().get(Function.NAME));
		
		Output<OUT> output = new Output<OUT>();
		try {
			AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
			dynamicBeanResolver.evaluate(function, Collections.<String, Object>unmodifiableMap(input.getProperties()));
			
			String handlerChain = functionType.getHandlerChain(function);
			FileVersionId handlerPackage = functionType.getHandlerPackage(function);

			// Build the property map used for the agent layer
			Map<String, String> inputMessageProperties = new HashMap<>();
			inputMessageProperties.put(FunctionMessageHandler.FUNCTION_HANDLER_KEY, handlerChain);
			if(handlerPackage != null) {
				inputMessageProperties.putAll(fileVersionIdToMap(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY, handlerPackage));
			}
			
			// Build the property map used for the function layer
			Map<String, String> properties = new HashMap<>();
			properties.putAll(input.getProperties());
			
			Map<String, String> handlerProperties = functionType.getHandlerProperties(function);
			if(handlerProperties!=null) {
				properties.putAll(handlerProperties);				
			}
			
			functionType.beforeFunctionCall(function, input, properties);
			
			input.setProperties(properties);

			int callTimeout = function.getCallTimeout().get();

			// Calculate the call timeout of the function. The offset is required to ensure that the call timeout of the function,
			// if used in the function handler, occurs before the agent timeout that will interrupt the thread
			if(callTimeout < 100) {
				throw new RuntimeException("The defined call timeout of the function should be higher than 100ms");
			}
			input.setFunctionCallTimeout(callTimeout-100l);

			// Serialize the input object
			JsonNode node = mapper.valueToTree(input);
			
			OutputMessage outputMessage;
			try {
				outputMessage = gridClient.call(tokenHandle, node, FunctionMessageHandler.class.getName(), functionHandlerPackage, inputMessageProperties, callTimeout);
			} catch (AgentCallTimeoutException e) {
				attachExceptionToOutput(output, "Timeout after " + callTimeout + "ms while calling the agent. You can increase the call timeout in the configuration screen of the keyword",e );
				return output;
			} catch (AgentSideException e) {
				attachExceptionToOutput(output, "Unexepected error on the agent side: "+e.getMessage(),e );
				return output;
			} catch (AgentCommunicationException e) {
				attachExceptionToOutput(output, "Communication error between the controller and the agent while calling the agent",e);
				return output;
			} 
			
			AgentError agentError = outputMessage.getAgentError();
			if(agentError != null) {
				AgentErrorCode errorCode = agentError.getErrorCode();
				if(errorCode.equals(AgentErrorCode.TIMEOUT_REQUEST_INTERRUPTED)) {
					output.setError("Timeout after " + callTimeout + "ms while executing the keyword on the agent. The keyword execution could be interrupted on the agent side. You can increase the call timeout in the configuration screen of the keyword");
				} else if(errorCode.equals(AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED)) {
					output.setError("Timeout after " + callTimeout + "ms while executing the keyword on the agent. WARNING: The keyword execution couldn't be interrupted on the agent side. You can increase the call timeout in the configuration screen of the keyword");
				} else if(errorCode.equals(AgentErrorCode.TOKEN_NOT_FOUND)) {
					output.setError("The agent token doesn't exist on the agent side");
				} else if(errorCode.equals(AgentErrorCode.UNEXPECTED)) {
					output.setError("Unexepected error while executing the keyword on the agent");
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER)) {
					output.setError("Unexpected error on the agent side while building the execution context of the keyword");
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER_FILE_PROVIDER_CALL_ERROR)) {
					output.setError("Error while downloading a resource from the controller");
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER_FILE_PROVIDER_CALL_TIMEOUT)) {
					String timeout = agentError.getErrorDetails().get(AgentErrorCode.Details.TIMEOUT);
					String filehandle = agentError.getErrorDetails().get(AgentErrorCode.Details.FILE_HANDLE);
					File file = gridClient.getRegisteredFile(filehandle);
					if(file!=null) {
						output.setError("Timeout after "+ timeout + "ms while downloading the following resource from the controller: "+file.getPath()+". You can increase the download timeout by setting gridReadTimeout in AgentConf.js");
					} else {
						output.setError("Timeout after "+ timeout + "ms while downloading a resource from the controller. You can increase the download timeout by setting gridReadTimeout in AgentConf.js");
					}
				} else {
					output.setError("Unknown agent error: "+agentError);
				}
			} else {
				output = mapper.treeToValue(outputMessage.getPayload(), Output.class);
			}
			
			if(outputMessage.getAttachments()!=null) {
				if(output.getAttachments()==null) {
					output.setAttachments(outputMessage.getAttachments());
				} else {
					output.getAttachments().addAll(outputMessage.getAttachments());					
				}
			}

			return output;
		} catch (Exception e) {
			if(logger.isDebugEnabled()) {
				logger.error("Unexpected error while calling function with id "+functionId, e);
			}
			attachExceptionToOutput(output, e);
		}
		return output;
	}
	
	protected Map<String, String> registerFile(File file, String properyName) {
		String fileHandle = gridClient.registerFile(file);
		return fileVersionIdToMap(properyName, new FileVersionId(fileHandle, FileHelper.getLastModificationDateRecursive(file)));
	}
	
	protected Map<String, String> fileVersionIdToMap(String propertyName, FileVersionId fileVersionId) {
		Map<String, String> props = new HashMap<>();
		props.put(propertyName+".id", fileVersionId.getFileId());
		props.put(propertyName+".version", Long.toString(fileVersionId.getVersion()));
		return props;
	}

	private void attachExceptionToOutput(Output<?> output, Exception e) {
		attachExceptionToOutput(output, "Unexpected error while calling keyword: " + e.getClass().getName() + " " + e.getMessage(), e);
	}
	
	private void attachExceptionToOutput(Output<?> output, String message, Exception e) {
		output.setError(message);
		Attachment attachment = AttachmentHelper.generateAttachmentForException(e);
		List<Attachment> attachments = output.getAttachments();
		if(attachments==null) {
			attachments = new ArrayList<>();
			output.setAttachments(attachments);
		}
		attachments.add(attachment);
	}
}
