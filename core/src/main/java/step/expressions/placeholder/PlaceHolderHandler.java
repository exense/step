package step.expressions.placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;

public class PlaceHolderHandler {
	private static Logger logger = LoggerFactory.getLogger(PlaceHolderHandler.class);
	
	private final Map<String, String> outputParameterMap;
	
	private List<Resolver> resolvers;
	
	public PlaceHolderHandler(ExecutionContext context, Map<String, String> outputParameterMap) {
		super();
		
		this.outputParameterMap = outputParameterMap;
		
		resolvers = new ArrayList<Resolver>();
		resolvers.add(new OutputParameterResolver(outputParameterMap));
		resolvers.add(new VariableResolver());
		resolvers.add(new GroovyDateResolver());
	}

	public Map<String, String> getOutputParameterMap() {
		return outputParameterMap;
	}
		
	public String resolve(String name) {
		if (name == null || name.isEmpty()){
			return "";
		}
		String result = null;
		for(Resolver resolver:resolvers) {
			result = resolver.resolve(name);
			if(result!=null) {
				logger.debug(resolver.getClass().getSimpleName() + " name: " + name + " was resolved to: " + result);
				return result;
			}
		}
		
		return null;
	}

	public String validate(String name) {
		// TODO Validation
		if (name == null || name.isEmpty()){
			return "";
		}
		String result = null;
		for(Resolver resolver:resolvers) {
			try {
				result = resolver.resolve(name);
				if(result!=null) {
					logger.debug(resolver.getClass().getSimpleName() + " name: " + name + " was resolved to: " + result);
					return result;
				}
			} catch (Exception e) {
				return name;
			}
			
		}
		
		return null;
	}
}
