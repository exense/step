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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.dynamicbeans.DynamicBeanResolver;
import step.functions.Function;
import step.functions.Input;
import step.functions.Output;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.client.GridClient;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionExecutionServiceImpl implements FunctionExecutionService {

	private final GridClient gridClient;
	
	private final FunctionAccessor functionAccessor;
	
	private final FunctionTypeRegistry functionTypeRegistry;
	
	private final DynamicBeanResolver dynamicBeanResolver;
	
	public FunctionExecutionServiceImpl(GridClient gridClient, FunctionAccessor functionAccessor,
			FunctionTypeRegistry functionTypeRegistry, DynamicBeanResolver dynamicBeanResolver) {
		super();
		this.gridClient = gridClient;
		this.functionAccessor = functionAccessor;
		this.functionTypeRegistry = functionTypeRegistry;
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	private static final Logger logger = LoggerFactory.getLogger(FunctionExecutionServiceImpl.class);
	
	@Override
	public TokenWrapper getLocalTokenHandle() {
		return gridClient.getLocalTokenHandle();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws AgentCommunicationException {
		return gridClient.getTokenHandle(attributes, interests, createSession);
	}

	@Override
	public void returnTokenHandle(TokenWrapper adapterToken) throws AgentCommunicationException {
		adapterToken.setCurrentOwner(null);
		gridClient.returnTokenHandle(adapterToken);
	}
	
	@Override
	public Output callFunction(TokenWrapper tokenHandle, Map<String,String> functionAttributes, Input input) {	
		Function function = functionAccessor.findByAttributes(functionAttributes);
		return callFunction(tokenHandle, function.getId().toString(), input);
	}
	
	@Override
	public Output callFunction(TokenWrapper tokenHandle, String functionId, Input input) {	
		Function function = functionAccessor.get(new ObjectId(functionId));
		
		Output output = new Output();
		output.setFunction(function);
		try {
			AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
			dynamicBeanResolver.evaluate(function, Collections.<String, Object>unmodifiableMap(input.getProperties()));
			
			String handlerChain = functionType.getHandlerChain(function);
			FileVersionId handlerPackage = functionType.getHandlerPackage(function);
			
			Map<String, String> properties = new HashMap<>();
			properties.putAll(input.getProperties());
			Map<String, String> handlerProperties = functionType.getHandlerProperties(function);
			if(handlerProperties!=null) {
				properties.putAll(handlerProperties);				
			}
			
			functionType.beforeFunctionCall(function, input, properties);
			
			int callTimeout = function.getCallTimeout().get();
			OutputMessage outputMessage = gridClient.call(tokenHandle, function.getAttributes().get(Function.NAME), input.getArgument(), handlerChain, handlerPackage, properties, callTimeout);
			
			output.setResult(outputMessage.getPayload());
			output.setError(outputMessage.getError());
			output.setAttachments(outputMessage.getAttachments());
			output.setMeasures(outputMessage.getMeasures());
			return output;
		} catch (Exception e) {
			if(logger.isDebugEnabled()) {
				logger.error("Error while calling function with id "+functionId, e);
			}
			attachExceptionToOutput(output, e);
		}
		return output;
	}

	public static void attachExceptionToOutput(Output output, Exception e) {
		output.setError("Unexpected error while calling function: " + e.getClass().getName() + " " + e.getMessage());
		Attachment attachment = AttachmentHelper.generateAttachmentForException(e);
		List<Attachment> attachments = output.getAttachments();
		if(attachments==null) {
			attachments = new ArrayList<>();
			output.setAttachments(attachments);
		}
		attachments.add(attachment);
	}
}
