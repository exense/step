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
import step.core.reports.Measure;
import step.core.reports.MeasurementsBuilder;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.ApplicationContextControl;
import step.grid.contextbuilder.LocalResourceApplicationContextFactory;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.threads.NamedThreadFactory;
import step.livereporting.client.LiveReportingClient;
import step.reporting.LiveReporting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FunctionMessageHandler extends AbstractMessageHandler {

	public static final String FUNCTION_HANDLER_PACKAGE_KEY = "$functionhandlerjar";
	public static final String FUNCTION_HANDLER_PACKAGE_CLEANABLE_KEY = "$functionhandlerjarCleanable";

	public static final String FUNCTION_HANDLER_KEY = "$functionhandler";
	public static final String FUNCTION_TYPE_KEY = "$functionType";
	public static final String BRANCH_HANDLER_INITIALIZER = "handler-initializer";

	// Cached object mapper for message payload serialization
	private final ObjectMapper mapper;

	private ThreadPoolExecutor liveReportingExecutor;
	// This is actually a Jakarta WebSocketContainer, but instantiated dynamically in a separate class loader
	private final AtomicReference<Object> webSocketContainerRef = new AtomicReference<>();
	private ApplicationContextBuilder applicationContextBuilder;

	public FunctionHandlerFactory functionHandlerFactory;

	public FunctionMessageHandler() {
		super();

		mapper = FunctionIOJavaxObjectMapperFactory.createObjectMapper();
	}

	private int getFromAgentPropsOrDefault(String configKey, int defaultValue) {
		Optional<String> agentPropsOverridingPoolSize = Optional.ofNullable(agentTokenServices.getAgentProperties())
				.map(m -> m.get(configKey));
		if (agentPropsOverridingPoolSize.isPresent()) {
			try {
				return Integer.parseInt(agentPropsOverridingPoolSize.get());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid agent properties override for " + configKey + ": " + agentPropsOverridingPoolSize.get());
			}
		}
		return defaultValue;
	}

	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);

		int liveReportingPoolSize = getFromAgentPropsOrDefault("step.reporting.livereporting.poolsize", 100);
		int liveReportingQueueSize = getFromAgentPropsOrDefault("step.reporting.livereporting.queuesize", 1000);

		// Behavior: This dynamically scales up/down between 0 and liveReportingPoolSize threads,
		// and if all threads are occupied, allows to queue a maximum of liveReportingQueueSize tasks before rejecting.
		liveReportingExecutor = new ThreadPoolExecutor(liveReportingPoolSize, liveReportingPoolSize,
				30L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(liveReportingQueueSize),
				NamedThreadFactory.create("livereporting", true)
		);
		liveReportingExecutor.allowCoreThreadTimeOut(true);


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
			JavaType javaType = mapper.getTypeFactory().constructParametricType(Input.class, functionHandler.getInputPayloadClass());
			Input<?> input = mapper.readValue(mapper.treeAsTokens(inputMessage.getPayload()), javaType);

			LiveReporting liveReporting = initializeLiveReporting(input.getProperties(), token.getTokenReservationSession());
			functionHandler.setLiveReporting(liveReporting);

			// Handle the input
			MeasurementsBuilder measurementsBuilder = new MeasurementsBuilder();
			measurementsBuilder.startMeasure(input.getFunction());

			@SuppressWarnings("unchecked")
			Output<?> output = functionHandler.handle(input);
			measurementsBuilder.stopMeasure(customMeasureData());

			liveReporting.close();

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

	private LiveReporting initializeLiveReporting(Map<String, String> properties, TokenReservationSession tokenReservationSession) throws Exception {
		ApplicationContextControl applicationContextControl = applicationContextBuilder.pushContext(BRANCH_HANDLER_INITIALIZER, new LocalResourceApplicationContextFactory(this.getClass().getClassLoader(), "step-functions-handler-initializer.jar"), true);
		// The usage of this application context will be released when the session is closed, underlying registered file won't be cleanable before this release happens
		tokenReservationSession.registerObjectToBeClosedWithSession(applicationContextControl);
		return applicationContextBuilder.runInContext(BRANCH_HANDLER_INITIALIZER, () -> {
			// There's no easy way to do this in the AbstractFunctionHandler itself, because
			// the only place where the Input properties are guaranteed to be available is in the (abstract)
			// handle() method (which would then have to be implemented in all subclasses). So we do it here.

			// Implementation class along with its dependencies is explicitly loaded in a separate classloader
			Class<?> liveReportingClientClass = Thread.currentThread().getContextClassLoader().loadClass("step.livereporting.client.RemoteLiveReportingClient");

			// This method invocation will also populate the websocketContainer reference if it isn't set yet
			Object liveReportingClient = liveReportingClientClass.getDeclaredConstructor(Map.class, Map.class, ExecutorService.class, AtomicReference.class)
					.newInstance(properties, agentTokenServices.getAgentProperties(), liveReportingExecutor, webSocketContainerRef);

			// We still need an additional proxy object to force everything to run in the correct context,
			// classloader separation alone is not enough.
			LiveReportingClient liveReportingClientProxy = (LiveReportingClient) Proxy.newProxyInstance(
					liveReportingClientClass.getClassLoader(), new Class[]{LiveReportingClient.class},
					(proxy, method, args) -> {
						try {
							return applicationContextBuilder.runInContext(BRANCH_HANDLER_INITIALIZER, () -> method.invoke(liveReportingClient, args));
						} catch (InvocationTargetException ite) {
							// rethrow the original exception instead of InvocationTargetException, (usually) conforming to the method's throws signature unless it's a RuntimeException or similar
							throw ite.getCause();
						}
					}
			);
			return new LiveReporting(liveReportingClientProxy.getStreamingUploadProvider(), liveReportingClientProxy.getLiveMeasureDestination());
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
		// The stop method of the websocket container has to be closed within its own context class loader
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(webSocketContainerRef.get().getClass().getClassLoader());
		try {
			Object webSocketContainer = webSocketContainerRef.getAndSet(null);
			if (webSocketContainer != null) {
				webSocketContainer.getClass().getMethod("stop").invoke(webSocketContainer);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
		}

		if (applicationContextBuilder != null) {
			applicationContextBuilder.close();
		}
		if (liveReportingExecutor != null) {
			liveReportingExecutor.shutdownNow();
		}
	}
}
