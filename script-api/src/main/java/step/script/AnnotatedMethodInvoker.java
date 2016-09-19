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
