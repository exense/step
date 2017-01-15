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
package step.functions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import step.core.GlobalContext;
import step.expressions.DynamicBeanResolver;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;
import step.functions.type.FunctionTypeConf;
import step.grid.AgentRef;
import step.grid.TokenWrapper;
import step.grid.client.GridClient;
import step.grid.client.GridClient.TokenHandle;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionClient {

	private final GridClient gridClient;
	
	private final FunctionRepository functionRepository;
	
	private final GlobalContext context;
	
	private final Map<String, AbstractFunctionType<FunctionTypeConf>> functionTypes = new HashMap<>();
	
	public FunctionClient(GlobalContext context, GridClient gridClient, FunctionRepository functionRepository) {
		super();
		this.context = context;
		this.gridClient = gridClient;
		this.functionRepository = functionRepository;
		
		loadFunctionTypes();
	}
	
	@SuppressWarnings({"unchecked" })
	private void loadFunctionTypes() {
		Set<Class<?>> functionTypeClasses = new Reflections("step").getTypesAnnotatedWith(FunctionType.class);
		
		for(Class<?> functionTypeClass:functionTypeClasses) {
			AbstractFunctionType<FunctionTypeConf> functionType;
			try {
				functionType = (AbstractFunctionType<FunctionTypeConf>) functionTypeClass.newInstance();
				functionType.setContext(context);
				functionType.init();
				functionTypes.put(functionTypeClass.getAnnotation(FunctionType.class).name(), functionType);
			} catch (InstantiationException | IllegalAccessException e) {
				
			}
		}
	}
	
	public static class FunctionTypeInfo {
		
		String name;
		
		String label;
		
		String editor;

		public FunctionTypeInfo(String name) {
			super();
			this.name = name;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getEditor() {
			return editor;
		}

		public void setEditor(String editor) {
			this.editor = editor;
		}
	}
	
	public Set<FunctionTypeInfo> getFunctionTypeInfos() {
		Set<FunctionTypeInfo> result = new HashSet<>();
		functionTypes.forEach((k,v)->
		{
			FunctionTypeInfo info = new FunctionTypeInfo(k);
			info.setLabel(v.getClass().getAnnotation(FunctionType.class).label());
			result.add(info);
		});
		return result;
	}
	
	public FunctionTypeConf newFunctionTypeConf(String functionType) {
		AbstractFunctionType<FunctionTypeConf> type = getFunctionTypeByName(functionType);
		return type.newFunctionTypeConf();
	}

	private AbstractFunctionType<FunctionTypeConf> getFunctionTypeByName(String functionType) {
		AbstractFunctionType<FunctionTypeConf> type = functionTypes.get(functionType);
		if(type==null) {
			throw new RuntimeException("Unknown function type '"+functionType+"'");
		} else {
			return type;
		}
	}
	
	public void setupFunction(Function function) {
		AbstractFunctionType<?> type = getFunctionTypeByName(function.getType());
		type.setupFunction(function);
	}
	
	public Function copyFunction(Function function) {
		AbstractFunctionType<?> type = getFunctionTypeByName(function.getType());
		return type.copyFunction(function);
	}
	
	public String getFunctionEditorPath(Function function) {
		AbstractFunctionType<?> type = getFunctionTypeByName(function.getType());
		return type.getEditorPath(function);
	}
	
	public FunctionTokenHandle getLocalFunctionToken() {
		return new FunctionTokenHandle(gridClient.getLocalToken());
	}

	public FunctionTokenHandle getFunctionToken(Map<String, String> attributes, Map<String, Interest> interest) {
		return new FunctionTokenHandle(gridClient.getToken(attributes, interest));
	}
	
	public FunctionTokenHandle getFunctionToken() {
		return new FunctionTokenHandle(gridClient.getToken());
	}
	
	public String registerAgentFile(File file) {
		return gridClient.registerFile(file);
	}
	
	public class FunctionTokenHandle {
		
		TokenHandle tokenHandle;

		private FunctionTokenHandle(TokenHandle tokenHandle) {
			super();
			this.tokenHandle = tokenHandle;
		}
		
		public Output call(Map<String, String> functionAttributes, Input input) {
			Function function = functionRepository.getFunctionByAttributes(functionAttributes);
			return callFunction(tokenHandle, function, input);
		}
		
		public Output call(String functionId, Input input) {
			Function function = functionRepository.getFunctionById(functionId);
			return callFunction(tokenHandle, function, input);
		}
		
		public void release() {
			tokenHandle.release();		
			tokenHandle.setCurrentOwner(null);
		}
		
		public void setCurrentOwner(Object owner) {
			tokenHandle.setCurrentOwner(owner);
		}

		public TokenHandle setDefaultCallTimeout(int callTimeout) {
			return tokenHandle.setCallTimeout(callTimeout);
		}

		@Override
		public String toString() {
			return tokenHandle.getToken().getID();
		}

		public AgentRef getAgentRef() {
			return tokenHandle.getToken().getAgent();
		}
		
		public TokenWrapper getToken() {
			return tokenHandle.getToken();
		}
		
		
	}

	private Output callFunction(TokenHandle tokenHandle, Function function, Input input) {
		Output output = new Output();
		output.setFunction(function);
		try {
			String functionTypeName = function.getType();
			AbstractFunctionType<FunctionTypeConf> functionType = getFunctionTypeByName(functionTypeName);
			FunctionTypeConf resolvedFunctionTypeConf = DynamicBeanResolver.resolveDynamicAttributes(function.getConfiguration(),Collections.<String, Object>unmodifiableMap(input.getProperties()));
			
			String handlerChain = functionType.getHandlerChain(function);
			Map<String, String> handlerProperties = functionType.getHandlerProperties(function);
			
			TokenHandle facade = tokenHandle.setHandler(handlerChain).addProperties(input.getProperties()).addProperties(handlerProperties);
			
			if(resolvedFunctionTypeConf.getCallTimeout()!=null) {
				facade.setCallTimeout(resolvedFunctionTypeConf.getCallTimeout());
			}

			OutputMessage outputMessage = facade.process(function.getAttributes().get("name"), input.getArgument());		
			output.setResult(outputMessage.getPayload());
			output.setError(outputMessage.getError());
			output.setAttachments(outputMessage.getAttachments());
			output.setMeasures(outputMessage.getMeasures());
			return output;
		} catch (Exception e) {
			output.setError("Unexpected error while calling function: " + e.getClass().getName() + " " + e.getMessage());
			Attachment attachment = AttachmentHelper.generateAttachmentForException(e);
			List<Attachment> attachments = output.getAttachments();
			if(attachments==null) {
				attachments = new ArrayList<>();
				output.setAttachments(attachments);
			}
			attachments.add(attachment);
		}
		return output;
	}

	public FunctionRepository getFunctionRepository() {
		return functionRepository;
	}
	
}
