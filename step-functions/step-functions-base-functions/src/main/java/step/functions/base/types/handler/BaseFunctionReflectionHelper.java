/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.functions.base.types.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import step.handlers.javahandler.Keyword;

/**
 * @author doriancransac
 *
 */
public class BaseFunctionReflectionHelper {

	public static final String LOCALFUNCTIONCLASSES_PREFIX = "step.functions.base.defaults";

	public static Map<String, String> getLocalKeywordsWithSchemas() throws Exception {

		Map<String,String> keywordList = new HashMap<>();

		try {
			Set<Method> methods = getLocalFunctionBaseReflections().getMethodsAnnotatedWith(Keyword.class);

			for(Method method:methods) {
				String schema = "{}";
				if(isImplementsInterface(method.getDeclaringClass().getGenericInterfaces(), BaseFunctionSchema.class)){
					BaseFunctionSchema keywordSchema = (BaseFunctionSchema)method.getDeclaringClass().newInstance();
					Map<String, String> schemas = keywordSchema.getKeywordSchemas();
					schema = schemas == null?"{}":schemas.get(method.getName());
				}
				keywordList.put(method.getName(), schema);
			}

			return keywordList;
		} catch (Exception e) {
			String errorMsg = "Error while looking for methods annotated with @Keyword in base classloader";
			throw new Exception(errorMsg, e);
		}
	}

	private static Reflections getLocalFunctionBaseReflections() {
		return new Reflections(LOCALFUNCTIONCLASSES_PREFIX, new MethodAnnotationsScanner());
	}

	private static boolean isImplementsInterface(Type[] genericInterfaces, Class<BaseFunctionSchema> class1) {
		for(Type type : genericInterfaces){
			if(type.getTypeName().equals(class1.getName()))
				return true;
		}
		return false;
	}

	public static List<String> getLocalKeywordList() throws Exception {

		List<String> keywordList = new ArrayList<>();

		try {
			Set<Method> methods = getLocalFunctionBaseReflections().getMethodsAnnotatedWith(Keyword.class);

			for(Method method:methods) {
				keywordList.add(method.getName());
			}

			return keywordList;
		} catch (Exception e) {
			String errorMsg = "Error while looking for methods annotated with @Keyword in base classloader";
			throw new Exception(errorMsg, e);
		}
	}

	public static Set<Class<?>> getLocalKeywordClasses() throws Exception {
		Set<Class<?>> classSet = new HashSet<>();

		for(Method m : getLocalFunctionBaseReflections().getMethodsAnnotatedWith(Keyword.class)){
			classSet.add(m.getDeclaringClass());
		}
		return classSet;
	}

	public static List<String> getLocalKeywordClassNames() throws Exception {
		List<String> classNames = new ArrayList<>();

		for(@SuppressWarnings("rawtypes") Class clazz : getLocalKeywordClasses()){
			classNames.add(clazz.getName());
		}
		return classNames;
	}

}
