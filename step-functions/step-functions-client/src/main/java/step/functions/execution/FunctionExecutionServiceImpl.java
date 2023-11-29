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
package step.functions.execution;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.functions.Function;
import step.functions.handler.*;
import step.functions.io.FunctionInput;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionExecutionException;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.client.AbstractGridClientImpl.AgentCallTimeoutException;
import step.grid.client.AbstractGridClientImpl.AgentCommunicationException;
import step.grid.client.AbstractGridClientImpl.AgentSideException;
import step.grid.client.GridClient;
import step.grid.client.GridClientException;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.io.*;
import step.grid.tokenpool.Interest;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FunctionExecutionServiceImpl implements FunctionExecutionService {

	public static final String INPUT_PROPERTY_DOCKER_IMAGE = "docker.image";
	public static final String INPUT_PROPERTY_CONTAINER_USER = "container.user";
	public static final String INPUT_PROPERTY_CONTAINER_CMD = "container.cmd";

	public static final String INPUT_PROPERTY_DOCKER_REGISTRY_URL = "docker.registryUrl";
	public static final String INPUT_PROPERTY_DOCKER_REGISTRY_USERNAME = "docker.registryUsername";
	public static final String INPUT_PROPERTY_DOCKER_REGISTRY_PASSWORD = "docker.registryPassword";


	private final GridClient gridClient;

	private final FunctionTypeRegistry functionTypeRegistry;

	private final DynamicBeanResolver dynamicBeanResolver;

	private final FileVersionId functionHandlerPackage;
	private final FileVersionId dockerHandlerPackageVersionId;

	private final ObjectMapper jakartaMapper;
	private final ObjectMapper javaxMapper;

	private static final String KEYWORD_NAME_PROP = "$keywordName";
	private static final String KEYWORD_TIMEOUT_PROP = "$keywordTimeout";

	public FunctionExecutionServiceImpl(GridClient gridClient, FunctionTypeRegistry functionTypeRegistry, DynamicBeanResolver dynamicBeanResolver) throws FunctionExecutionServiceException {
		super();
		this.gridClient = gridClient;
		this.functionTypeRegistry = functionTypeRegistry;
		this.dynamicBeanResolver = dynamicBeanResolver;

		functionHandlerPackage = registerClassloaderResource("step-functions-handler.jar");
		dockerHandlerPackageVersionId = registerClassloaderResource("step-functions-docker-handler.jar");

		jakartaMapper = FunctionIOJakartaObjectMapperFactory.createObjectMapper();
		javaxMapper = FunctionIOJavaxObjectMapperFactory.createObjectMapper();
	}

	private FileVersionId registerClassloaderResource(String functionHandlerResourceName) throws FunctionExecutionServiceException {
		FileVersion functionHandlerPackageVersionId;
		try {
			InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(functionHandlerResourceName);
			functionHandlerPackageVersionId = gridClient.registerFile(resourceAsStream, functionHandlerResourceName, false, false);
		} catch (FileManagerException e) {
			throw new FunctionExecutionServiceException("Error while registering file "+ functionHandlerResourceName +" to the grid", e);
		}
		return functionHandlerPackageVersionId.getVersionId();
	}

	private static final Logger logger = LoggerFactory.getLogger(FunctionExecutionServiceImpl.class);

	private final List<TokenLifecycleInterceptor> tokenLifecycleInterceptors = new CopyOnWriteArrayList<>();

	@Override
	public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {
		tokenLifecycleInterceptors.add(interceptor);
	}


	@Override
	public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {
		tokenLifecycleInterceptors.remove(interceptor);
	}

	@Override
	public TokenWrapper getLocalTokenHandle() {
		return gridClient.getLocalTokenHandle();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
		TokenWrapper tokenWrapper = null;
		try {
			tokenWrapper = gridClient.getTokenHandle(attributes, interests, createSession, tokenWrapperOwner);
		} catch (AgentCallTimeoutException e) {
			throw new FunctionExecutionServiceException("Timeout after "+e.getCallTimeout()+"ms while reserving the agent token. You can increase the call timeout by setting 'grid.client.token.reserve.timeout.ms' in step.properties",e );
		} catch (AgentSideException e) {
			throw new FunctionExecutionServiceException("Unexpected error on the agent side while reserving the agent token: "+e.getMessage(),e);
		} catch (AgentCommunicationException e) {
			throw new FunctionExecutionServiceException("Communication error between the controller and the agent while reserving the agent token",e);
		}

		try {
            // See TokenLifecycleInterceptor javadoc for more details.
            for (TokenLifecycleInterceptor interceptor : tokenLifecycleInterceptors) {
				interceptor.onGetTokenHandle(tokenWrapper.getID());
			}
		} catch (Exception e) {
			try {
				returnTokenHandle(tokenWrapper.getID());
			} catch (Exception ignored) {
				logger.warn("Unexpected error while returning token handle, ignoring: ", e);
			}
			throw new FunctionExecutionServiceException("Error while retrieving agent token: " + e.getMessage(), e);
		}
		return tokenWrapper;
	}

    @Override
	public void returnTokenHandle(String tokenHandleId) throws FunctionExecutionServiceException {
        for (TokenLifecycleInterceptor interceptor : tokenLifecycleInterceptors) {
            try {
                interceptor.onReturnTokenHandle(tokenHandleId);
            } catch (Exception unexpected) {
                logger.error("Unexpected exception in token handle interceptor " + interceptor +", ignoring", unexpected);
            }
        }
		try {
			gridClient.returnTokenHandle(tokenHandleId);
		} catch (AgentCallTimeoutException e) {
			throw new FunctionExecutionServiceException("Timeout after "+e.getCallTimeout()+"ms while releasing the agent token. You can increase the call timeout by setting 'grid.client.token.release.timeout.ms' in step.properties",e );
		} catch (AgentSideException e) {
			throw new FunctionExecutionServiceException("Unexpected error on the agent side while releasing the agent token: "+e.getMessage(),e);
		} catch (AgentCommunicationException e) {
			throw new FunctionExecutionServiceException("Communication error between the controller and the agent while releasing the agent token",e);
		} catch (GridClientException e) {
			throw new FunctionExecutionServiceException("Unexpected error while releasing the agent token: "+e.getMessage(),e);
		}
	}

	@Override
	public <IN,OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass) {
		Input<IN> input = new Input<>();
		input.setPayload(functionInput.getPayload());
		input.setFunction(function.getAttributes().get(AbstractOrganizableObject.NAME));

		// Build the property map used for the function layer
		Map<String, String> properties = new HashMap<>();
		if(functionInput.getProperties() !=null) {
			properties.putAll(functionInput.getProperties());
		}

		Output<OUT> output = new Output<OUT>();
		try {
			AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
			dynamicBeanResolver.evaluate(function, Collections.<String, Object>unmodifiableMap(properties));

			String handlerChain = functionType.getHandlerChain(function);
			FileVersionId handlerPackage = functionType.getHandlerPackage(function);

			// Build the property map used for the agent layer
			Map<String, String> messageProperties = new HashMap<>();
			messageProperties.put(FunctionMessageHandler.FUNCTION_HANDLER_KEY, handlerChain);
			if(handlerPackage != null) {
				messageProperties.putAll(fileVersionIdToMap(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY, handlerPackage));
			}


			Map<String, String> handlerProperties = functionType.getHandlerProperties(function);
			if(handlerProperties!=null) {
				properties.putAll(handlerProperties);
			}

			functionType.beforeFunctionCall(function, input, properties);

			input.setProperties(properties);

			int callTimeout = function.getCallTimeout().get();

			//expose additional properties to the keyword
			properties.put(KEYWORD_NAME_PROP, input.getFunction());
			properties.put(KEYWORD_TIMEOUT_PROP, Integer.toString(callTimeout));

			// Calculate the call timeout of the function. The offset is required to ensure that the call timeout of the function,
			// if used in the function handler, occurs before the agent timeout that will interrupt the thread
			if(callTimeout < 100) {
				throw new RuntimeException("The defined call timeout of the function should be higher than 100ms");
			}
			input.setFunctionCallTimeout(callTimeout-100l);

			// Serialize the input object
			JsonNode node = jakartaMapper.valueToTree(input);

			String functionMessageHandler = FunctionMessageHandler.class.getName();
			boolean inDocker = properties.containsKey(INPUT_PROPERTY_DOCKER_IMAGE);

			String messageHandler;
			FileVersionId messageHandlerPackage;
			OutputMessage outputMessage;
			try {
				if(inDocker) {
					// Using the proxy message handler in order to forward calls to the sub agent
					messageHandler = ProxyMessageHandler.class.getName();
					messageHandlerPackage = dockerHandlerPackageVersionId;
					messageProperties.put(ProxyMessageHandler.MESSAGE_HANDLER, functionMessageHandler);
					messageProperties.put(ProxyMessageHandler.MESSAGE_HANDLER_FILE_ID, functionHandlerPackage.getFileId());
					messageProperties.put(ProxyMessageHandler.MESSAGE_HANDLER_FILE_VERSION, functionHandlerPackage.getVersion());

					messageProperties.put(DockerContainer.MESSAGE_PROP_DOCKER_REGISTRY_URL, properties.get(INPUT_PROPERTY_DOCKER_REGISTRY_URL));
					messageProperties.put(DockerContainer.MESSAGE_PROP_DOCKER_REGISTRY_USERNAME, properties.get(INPUT_PROPERTY_DOCKER_REGISTRY_USERNAME));
					messageProperties.put(DockerContainer.MESSAGE_PROP_DOCKER_REGISTRY_PASSWORD, properties.get(INPUT_PROPERTY_DOCKER_REGISTRY_PASSWORD));
					messageProperties.put(DockerContainer.MESSAGE_PROP_DOCKER_IMAGE, properties.get(INPUT_PROPERTY_DOCKER_IMAGE));
					messageProperties.put(DockerContainer.MESSAGE_PROP_CONTAINER_USER, properties.get(INPUT_PROPERTY_CONTAINER_USER));
					messageProperties.put(DockerContainer.MESSAGE_PROP_CONTAINER_CMD, properties.get(INPUT_PROPERTY_CONTAINER_CMD));
				} else {
					messageHandler = functionMessageHandler;
					messageHandlerPackage = functionHandlerPackage;
				}
				outputMessage = gridClient.call(tokenHandleId, node, messageHandler, messageHandlerPackage, messageProperties, callTimeout);
			} catch (AgentCallTimeoutException e) {
				attachUnexpectedExceptionToOutput(output, "Timeout after " + callTimeout + "ms while calling the agent. You can increase the call timeout in the configuration screen of the keyword",e );
				return output;
			} catch (AgentSideException e) {
				attachUnexpectedExceptionToOutput(output, "Unexpected error on the agent side: "+e.getMessage(),e );
				return output;
			} catch (AgentCommunicationException e) {
				attachUnexpectedExceptionToOutput(output, "Communication error between the controller and the agent while calling the agent",e);
				return output;
			}

			AgentError agentError = outputMessage.getAgentError();
			if(agentError != null) {
				AgentErrorCode errorCode = agentError.getErrorCode();
				if(errorCode.equals(AgentErrorCode.TIMEOUT_REQUEST_INTERRUPTED)) {
					output.setError(newAgentError("Timeout after " + callTimeout + "ms while executing the keyword on the agent. The keyword execution could be interrupted on the agent side. You can increase the call timeout in the configuration screen of the keyword"));
				} else if(errorCode.equals(AgentErrorCode.TIMEOUT_REQUEST_NOT_INTERRUPTED)) {
					output.setError(newAgentError("Timeout after " + callTimeout + "ms while executing the keyword on the agent. WARNING: The keyword execution couldn't be interrupted on the agent side. You can increase the call timeout in the configuration screen of the keyword"));
				} else if(errorCode.equals(AgentErrorCode.TOKEN_NOT_FOUND)) {
					output.setError(newAgentError("The agent token doesn't exist on the agent side"));
				} else if(errorCode.equals(AgentErrorCode.UNEXPECTED)) {
					output.setError(newAgentError("Unexpected error while executing the keyword on the agent"));
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER)) {
					output.setError(newAgentError("Unexpected error on the agent side while building the execution context of the keyword"));
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER_FILE_PROVIDER_CALL_ERROR)) {
					output.setError(newAgentError("Error while downloading a resource from the controller"));
				} else if(errorCode.equals(AgentErrorCode.CONTEXT_BUILDER_FILE_PROVIDER_CALL_TIMEOUT)) {
					String timeout = agentError.getErrorDetails().get(AgentErrorCode.Details.TIMEOUT);
					String filehandle = agentError.getErrorDetails().get(AgentErrorCode.Details.FILE_HANDLE);
					String fileversion = agentError.getErrorDetails().get(AgentErrorCode.Details.FILE_VERSION);
					FileVersion fileVersion = gridClient.getRegisteredFile(new FileVersionId(filehandle, fileversion));
					if(fileVersion!=null) {
						output.setError(newAgentError("Timeout after "+ timeout + "ms while downloading the following resource from the controller: "+fileVersion.getFile().getPath()+". You can increase the download timeout by setting gridReadTimeout in AgentConf.js"));
					} else {
						output.setError(newAgentError("Timeout after "+ timeout + "ms while downloading a resource from the controller. You can increase the download timeout by setting gridReadTimeout in AgentConf.js"));
					}
				} else {
					output.setError(newAgentError("Unknown agent error: "+agentError));
				}
			} else {
				JavaType javaType = jakartaMapper.getTypeFactory().constructParametrizedType(Output.class, Output.class, outputClass);
				if (outputClass.getName().equals("javax.json.JsonObject")) {
					output = javaxMapper.readValue(jakartaMapper.treeAsTokens(outputMessage.getPayload()), javaType);
				} else {
					output = jakartaMapper.readValue(jakartaMapper.treeAsTokens(outputMessage.getPayload()), javaType);
				}
			}

			if(outputMessage.getAttachments()!=null) {
				if(output.getAttachments()==null) {
					output.setAttachments(outputMessage.getAttachments());
				} else {
					output.getAttachments().addAll(outputMessage.getAttachments());
				}
			}

			return output;
		} catch (FunctionExecutionException e) {
			output.setError(e.getError());
			Exception source = e.getSource();
			if(source != null) {
				attachExceptionToOutput(output, source);
			}
		} catch (Exception e) {
			if(logger.isDebugEnabled()) {
				logger.error("Unexpected error while calling function with id "+function.getId().toString(), e);
			}
			attachUnexpectedExceptionToOutput(output, e);
		}
		return output;
	}

	private Error newAgentError(String message) {
		return new Error(ErrorType.TECHNICAL, "agent", message, 0, true);
	}

	/**
	 * Register the provided file in the grid's file manager
	 *
	 * @param file the {@link File} of the resource to be registered
	 * @param propertyName the name of the property for which we register the file
	 * @param cleanable if this version of the file can be cleaned-up at runtime
	 * @return the FileVersionId as a property map of the registered file.
	 * @throws FileManagerException
	 */
	protected Map<String, String> registerFile(File file, String propertyName, boolean cleanable) throws FileManagerException {
		FileVersion fileVersion = gridClient.registerFile(file, cleanable);
		return fileVersionIdToMap(propertyName, fileVersion.getVersionId());
	}

	protected Map<String, String> fileVersionIdToMap(String propertyName, FileVersionId fileVersionId) {
		Map<String, String> props = new HashMap<>();
		props.put(propertyName+".id", fileVersionId.getFileId());
		props.put(propertyName+".version", fileVersionId.getVersion());
		return props;
	}

	private void attachUnexpectedExceptionToOutput(Output<?> output, Exception e) {
		attachUnexpectedExceptionToOutput(output, "Unexpected error while calling keyword: " + e.getClass().getName() + " " + e.getMessage(), e);
	}

	private void attachUnexpectedExceptionToOutput(Output<?> output, String message, Exception e) {
		output.setError( new Error(ErrorType.TECHNICAL, "functionClient", message, 0, true));
		attachExceptionToOutput(output, e);
	}

	private void attachExceptionToOutput(Output<?> output, Exception e) {
		Attachment attachment = AttachmentHelper.generateAttachmentForException(e);
		List<Attachment> attachments = output.getAttachments();
		if(attachments==null) {
			attachments = new ArrayList<>();
			output.setAttachments(attachments);
		}
		attachments.add(attachment);
	}
}
