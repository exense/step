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
package step.core.dynamicbeans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("rawtypes")
public class DynamicBeanResolver {
	
	private static final Logger logger = LoggerFactory.getLogger(DynamicBeanResolver.class);
	
	private final DynamicValueResolver valueResolver;

	private final Map<Class<?>,BeanInfo> beanInfoCache = new ConcurrentHashMap<>();

	public DynamicBeanResolver(DynamicValueResolver valueResolver) {
		super();
		this.valueResolver = valueResolver;
	}

	public void evaluate(Object o, Map<String, Object> bindings) {
		if(o!=null) {
			Class<?> clazz = o.getClass();
			
			try {
				BeanInfo beanInfo = beanInfoCache.get(clazz);
				if(beanInfo==null) {
					beanInfo = Introspector.getBeanInfo(clazz, Object.class);
					beanInfoCache.put(clazz, beanInfo);
				}

				// Handle public fields
				for (Field field:clazz.getFields()) {
					int modifiers = field.getModifiers();
					if(!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
						if(field.getType().equals(DynamicValue.class)) {
							Object value = field.get(o);
							evaluateDynamicValue(bindings, (DynamicValue<?>) value);
						} else if(field.isAnnotationPresent(ContainsDynamicValues.class)) {
							Object value = field.get(o);
							recursivelyEvaluateValue(bindings, value);
						}
					}
				}

				// Handle fields with getter / setter
				for(PropertyDescriptor descriptor:beanInfo.getPropertyDescriptors()) {
					Method method = descriptor.getReadMethod();
					if(method!=null) {
						if(method.getReturnType().equals(DynamicValue.class)) {
							Object value = method.invoke(o);
							evaluateDynamicValue(bindings, (DynamicValue<?>) value);
						} else if(method.isAnnotationPresent(ContainsDynamicValues.class)) {
							Object value = method.invoke(o);
							recursivelyEvaluateValue(bindings, value);
						}
					}
				}
			} catch (Exception e) {
				if(logger.isDebugEnabled()) {
					logger.debug("Error while evaluating object: "+ o, e);
				}
			}			
		}
	}

	private void recursivelyEvaluateValue(Map<String, Object> bindings, Object value) {
		if (value instanceof  List) {
			List l = (List) value;
			l.forEach(v -> evaluate(v, bindings));
		} else {
			evaluate(value, bindings);
		}
	}

	private void evaluateDynamicValue(Map<String, Object> bindings, DynamicValue<?> value) {
		if(value!=null) {
			valueResolver.evaluate(value, bindings);
			evaluate(value.get(), bindings);
		}
	}

	public <T> T cloneDynamicValues(T o) {
		if(o!=null) {
			try {
				Class<?> clazz = o.getClass();
				@SuppressWarnings("unchecked")
				T out = (T) clazz.getConstructor().newInstance();
				if (List.class.isAssignableFrom(clazz)) {
					List l = (List) o;
					List outList = (List) out;
					l.forEach(c -> outList.add(cloneDynamicValues(c)));
				} else {
					BeanInfo beanInfo = beanInfoCache.get(clazz);
					if (beanInfo == null) {
						beanInfo = Introspector.getBeanInfo(clazz, Object.class);
						beanInfoCache.put(clazz, beanInfo);
					}

					// Handle public fields
					for (Field field:clazz.getFields()) {
						int modifiers = field.getModifiers();
						if(!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
							Object oldValue = field.get(o);
							Object newValue = cloneDynamicValue(field, oldValue);
							field.set(out, newValue);
						}
					}

					// Handle fields with getters / setters
					for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
						Method method = descriptor.getReadMethod();
						if (method != null) {
							Object oldValue = method.invoke(o);
							Object newValue = cloneDynamicValue(method, oldValue);
							Method writeMethod = descriptor.getWriteMethod();
							if (writeMethod != null) {
								descriptor.getWriteMethod().invoke(out, newValue);
							}
						}
					}
				}
				return out;
			} catch (Exception e) {
				throw new RuntimeException("Error while cloning object "+ o,e);
			} 
		} else {
			return null;
		}
	}

	private Object cloneDynamicValue(AccessibleObject fieldOrMethod, Object oldValue) {
		Object newValue;
		if (oldValue != null) {
			if (oldValue instanceof DynamicValue) {
				DynamicValue<?> dynamicValue = (DynamicValue<?>) oldValue;
				newValue = dynamicValue.cloneValue();
			} else if (fieldOrMethod.isAnnotationPresent(ContainsDynamicValues.class)) {
				newValue = cloneDynamicValues(oldValue);
			} else {
				newValue = oldValue;
			}
		} else {
			newValue = null;
		}
		return newValue;
	}
}
