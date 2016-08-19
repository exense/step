package step.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class AnnotatedMethodInvoker {

	{
		Configuration.setDefaults(new Configuration.Defaults() {

		    private final JsonProvider jsonProvider = new JacksonJsonProvider();
		    private final MappingProvider mappingProvider = new JacksonMappingProvider();

		    @Override
		    public JsonProvider jsonProvider() {
		        return jsonProvider;
		    }

		    @Override
		    public MappingProvider mappingProvider() {
		        return mappingProvider;
		    }

		    @Override
		    public Set<Option> options() {
		        return EnumSet.noneOf(Option.class);
		    }
		});
		
	}
	
	public static Object invoke(Object object, Method method, String argument)
			throws InstantiationException, IllegalAccessException, InvocationTargetException {
		return invoke(object, method, argument, null);
	}
	
	public static Object invoke(Object object, Method method, String argument, Map<String, String> properties) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ReadContext docCtx = null;
		if(argument!=null) {
			docCtx = JsonPath.parse(argument);
		}
		
		int paramNum = 0;
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];
		for(Parameter parameter:parameters) {
			Object value=null;
			for(Annotation a:parameter.getAnnotations()) {
				if(a instanceof Arg) {
					String jsonPath = ((Arg)a).value();
					try {
						value = docCtx.read(jsonPath, parameter.getType());
					} catch(PathNotFoundException e) {
						value = null;
					}
				} else if(a instanceof Prop) {
					String key = ((Prop)a).value();
					if(key==null) {
						if(parameter.getType().isAssignableFrom(Map.class)) {
							value = properties;
						}
					} else {
						value = properties.get(key);													
					}
				}
			}
			
			args[paramNum] = value;
			paramNum++;
		}
		
		return method.invoke(object, args);
	}

}
