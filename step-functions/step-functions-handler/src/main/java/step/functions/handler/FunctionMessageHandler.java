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
package step.functions.handler;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.constants.StreamingConstants;
import step.core.reports.Measure;
import step.core.reports.MeasurementsBuilder;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.LocalResourceApplicationContextFactory;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.reporting.LiveReporting;
import step.streaming.client.upload.StreamingUploadProvider;
import step.streaming.common.StreamingResourceUploadContext;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionMessageHandler extends AbstractMessageHandler {

	public static final String FUNCTION_HANDLER_PACKAGE_KEY = "$functionhandlerjar";
	public static final String FUNCTION_HANDLER_PACKAGE_CLEANABLE_KEY = "$functionhandlerjarCleanable";

	public static final String FUNCTION_HANDLER_KEY = "$functionhandler";
	public static final String FUNCTION_TYPE_KEY = "$functionType";
	public static final String BRANCH_HANDLER_INITIALIZER = "handler-initializer";

	// Cached object mapper for message payload serialization
	private final ObjectMapper mapper;

	private ApplicationContextBuilder applicationContextBuilder;

	public FunctionHandlerFactory functionHandlerFactory;

	public FunctionMessageHandler() {
		super();

		mapper = FunctionIOJavaxObjectMapperFactory.createObjectMapper();
	}

	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		applicationContextBuilder = new ApplicationContextBuilder(this.getClass().getClassLoader(),
				agentTokenServices.getApplicationContextBuilder().getApplicationContextConfiguration());

		applicationContextBuilder.forkCurrentContext(AbstractFunctionHandler.FORKED_BRANCH);
		applicationContextBuilder.forkCurrentContext(BRANCH_HANDLER_INITIALIZER);

		functionHandlerFactory = new FunctionHandlerFactory(applicationContextBuilder, agentTokenServices.getFileManagerClient());
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage inputMessage) throws Exception {
		applicationContextBuilder.resetContext();
		FileVersionId functionPackage = getFileVersionId(FUNCTION_HANDLER_PACKAGE_KEY, inputMessage.getProperties());
		if(functionPackage != null) {
			boolean cleanable = Boolean.parseBoolean(inputMessage.getProperties().get(FUNCTION_HANDLER_PACKAGE_CLEANABLE_KEY));
			RemoteApplicationContextFactory functionHandlerContext = new RemoteApplicationContextFactory(token.getServices().getFileManagerClient(), functionPackage, cleanable);
			// The usage of this functionHandlerContext will only be released when the session is closed, underlying registered file won't be cleanable before this release happens
			token.getTokenReservationSession().registerObjectToBeClosedWithSession(applicationContextBuilder.pushContext(functionHandlerContext, cleanable));
		}

		return applicationContextBuilder.runInContext(() -> {
			// Merge the token and agent properties
			Map<String, String> mergedAgentProperties = getMergedAgentProperties(token);
			// Instantiate the function handler
			String handlerClass = inputMessage.getProperties().get(FUNCTION_HANDLER_KEY);

			if (handlerClass == null || handlerClass.isEmpty()) {
				String msg = "There is no supported handler for function type \"" + inputMessage.getProperties().get(FUNCTION_TYPE_KEY) + "\"";
				throw new UnsupportedOperationException(msg);
			}

			@SuppressWarnings("rawtypes")
			AbstractFunctionHandler functionHandler = functionHandlerFactory.create(applicationContextBuilder.getCurrentContext().getClassLoader(),
					handlerClass, token.getSession(), token.getTokenReservationSession(), mergedAgentProperties);

			// Deserialize the Input from the message payload
			JavaType javaType = mapper.getTypeFactory().constructParametrizedType(Input.class, Input.class, functionHandler.getInputPayloadClass());
			Input<?> input = mapper.readValue(mapper.treeAsTokens(inputMessage.getPayload()), javaType);

			functionHandler.setLiveReporting(initializeLiveReporting(input.getProperties()));

			// Handle the input
			MeasurementsBuilder measurementsBuilder = new MeasurementsBuilder();
			measurementsBuilder.startMeasure(input.getFunction());

			@SuppressWarnings("unchecked")
			Output<?> output = functionHandler.handle(input);
			measurementsBuilder.stopMeasure(customMeasureData());

			List<Measure> outputMeasures = output.getMeasures();
			// Add type="custom" to all output measures
			addCustomTypeToOutputMeasures(outputMeasures);

			// Add Keyword measure to output
			addAdditionalMeasuresToOutput(output, measurementsBuilder.getMeasures());

			// Serialize the output
			ObjectNode outputPayload = mapper.valueToTree(output);

			// Create and return the output message
			OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
			outputMessageBuilder.setPayload(outputPayload);
			return outputMessageBuilder.build();

		});
	}

	private LiveReporting initializeLiveReporting(Map<String, String> properties) throws Exception {
		applicationContextBuilder.pushContext(BRANCH_HANDLER_INITIALIZER, new LocalResourceApplicationContextFactory(this.getClass().getClassLoader(), "step-functions-handler-initializer.jar"), true);
		return applicationContextBuilder.runInContext(BRANCH_HANDLER_INITIALIZER, () -> {
			// There's no easy way to do this in the AbstractFunctionHandler itself, because
			// the only place where the Input properties are guaranteed to be available is in the (abstract)
			// handle() method (which would then have to be implemented in all subclasses). So we do it here.

			// We currently only support Websocket uploads; if this changes in the future, here is the place to modify the logic.
			String uploadContextId = properties.get(StreamingResourceUploadContext.PARAMETER_NAME);
			if (uploadContextId != null) {
				// This information could also be retrieved from somewhere else (e.g. this.agentTokenServices....),
				// for now it's in the inputs provided by the controller itself.
				String host = properties.get(StreamingConstants.AttributeNames.WEBSOCKET_BASE_URL);
				while (host.endsWith("/")) {
					host = host.substring(0, host.length() - 1);
				}
				String path = properties.get(StreamingConstants.AttributeNames.WEBSOCKET_UPLOAD_PATH);
				while (path.startsWith("/")) {
					path = path.substring(1);
				}
				URI uri = URI.create(String.format("%s/%s?%s=%s", host, path, StreamingResourceUploadContext.PARAMETER_NAME, uploadContextId));

				@SuppressWarnings("unchecked") Class<StreamingUploadProvider> aClass = (Class<StreamingUploadProvider>) Thread.currentThread().getContextClassLoader().loadClass("step.streaming.websocket.client.upload.WebsocketUploadProvider");
				StreamingUploadProvider streamingUploadProvider = aClass.getDeclaredConstructor(URI.class).newInstance(uri);

				StreamingUploadProvider proxiedProvider = (StreamingUploadProvider) Proxy.newProxyInstance(
						aClass.getClassLoader(), new Class[]{StreamingUploadProvider.class},
						(proxy, method, args) -> applicationContextBuilder.runInContext(BRANCH_HANDLER_INITIALIZER, () -> method.invoke(streamingUploadProvider, args))
				);
				return new LiveReporting(proxiedProvider);
			} else {
				// This will now fall back to auto-determining a streaming provider, usually one that discards all uploads. See the API implementation for details.
				return new LiveReporting(null);
			}
		});
	}

	protected void addCustomTypeToOutputMeasures(List<Measure> outputMeasures) {
		if(outputMeasures!=null) {
			outputMeasures.forEach(m->{
				Map<String, Object> attributes = new HashMap<>();
				if (m.getData() != null ) {
					attributes.putAll(m.getData());
				}
				attributes.putIfAbsent(MeasureTypes.ATTRIBUTE_TYPE, MeasureTypes.TYPE_CUSTOM);
				m.setData(attributes);
			});
		}
	}

	protected void addAdditionalMeasuresToOutput(Output<?> output, List<Measure> additionalMeasures) {
		List<Measure> outputMeasures = output.getMeasures();
		if(outputMeasures == null) {
			output.setMeasures(additionalMeasures);
		} else {
			outputMeasures.addAll(additionalMeasures);
		}
	}

	protected Map<String, Object> customMeasureData() {
		Map<String, Object> data = new HashMap<>();
		data.put(MeasureTypes.ATTRIBUTE_TYPE, MeasureTypes.TYPE_KEYWORD);
		return data;
	}

	private Map<String, String> getMergedAgentProperties(AgentTokenWrapper token) {
		Map<String, String> mergedAgentProperties = new HashMap<>();
		Map<String, String> agentProperties = token.getServices().getAgentProperties();
		if(agentProperties !=null) {
			mergedAgentProperties.putAll(agentProperties);
		}
		Map<String, String> tokenProperties = token.getProperties();
		if(tokenProperties != null) {
			mergedAgentProperties.putAll(tokenProperties);
		}
		return mergedAgentProperties;
	}

	@Override
	public void close() throws Exception {
		if (applicationContextBuilder != null) {
			applicationContextBuilder.close();
		}
	}
}
