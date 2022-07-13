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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class FunctionMessageHandler extends AbstractMessageHandler {

	public static final String FUNCTION_HANDLER_PACKAGE_KEY = "$functionhandlerjar";
	
	public static final String FUNCTION_HANDLER_KEY = "$functionhandler";
	
	// Cached object mapper for message payload serialization
	private ObjectMapper mapper;
	
	private ApplicationContextBuilder applicationContextBuilder;
	
	public FunctionHandlerFactory functionHandlerFactory;
	
	public FunctionMessageHandler() {
		super();
		
		mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
	}

	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		applicationContextBuilder = new ApplicationContextBuilder(this.getClass().getClassLoader());
		
		applicationContextBuilder.forkCurrentContext(AbstractFunctionHandler.FORKED_BRANCH);
		
		functionHandlerFactory = new FunctionHandlerFactory(applicationContextBuilder, agentTokenServices.getFileManagerClient());
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage inputMessage) throws Exception {
		applicationContextBuilder.resetContext();
		
		FileVersionId functionPackage = getFileVersionId(FUNCTION_HANDLER_PACKAGE_KEY, inputMessage.getProperties());
		if(functionPackage != null) {
			RemoteApplicationContextFactory functionHandlerContext = new RemoteApplicationContextFactory(token.getServices().getFileManagerClient(), getFileVersionId(FUNCTION_HANDLER_PACKAGE_KEY, inputMessage.getProperties()));
			applicationContextBuilder.pushContext(functionHandlerContext);
		}
		
		return applicationContextBuilder.runInContext(()->{
			// Merge the token and agent properties
			Map<String, String> mergedAgentProperties = getMergedAgentProperties(token);
			// Instantiate the function handler 
			String handlerClass = inputMessage.getProperties().get(FUNCTION_HANDLER_KEY);
			@SuppressWarnings("rawtypes")
			AbstractFunctionHandler functionHandler = functionHandlerFactory.create(applicationContextBuilder.getCurrentContext().getClassLoader(), 
					handlerClass, token.getSession(), token.getTokenReservationSession(), mergedAgentProperties);
			
			// Deserialize the Input from the message payload
			JavaType javaType = mapper.getTypeFactory().constructParametrizedType(Input.class, Input.class, functionHandler.getInputPayloadClass());
			Input<?> input = mapper.readValue(mapper.treeAsTokens(inputMessage.getPayload()), javaType);
			
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
			ObjectNode outputPayload = (ObjectNode) mapper.valueToTree(output);

			// Create and return the output message 
			OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
			outputMessageBuilder.setPayload(outputPayload);
			return outputMessageBuilder.build();
			
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
}
