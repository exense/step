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

public class ValueConverter {

	@SuppressWarnings("unchecked")
	public static <T> T convert(Object value, Class<T> classTo) {
		if(value != null) {
			if(String.class.isAssignableFrom(classTo)) {
				return (T) value.toString();
			} else if(Long.class.isAssignableFrom(classTo)) {
				if(value instanceof Number) {
					return (T)new Long(((Number)value).longValue());
				} else if(value instanceof String){
					return (T)new Long(Long.parseLong((String)value));
				} else {
					throw unsupportedConversionException(classTo, value);
				}
			} else if(Integer.class.isAssignableFrom(classTo)) {
				if(value instanceof Number) {
					return (T)new Integer(((Number)value).intValue());
				} else if(value instanceof String){
					return (T)new Integer(Integer.parseInt((String)value));
				} else {
					throw unsupportedConversionException(classTo, value);
				}
			} else {
				throw unsupportedConversionException(classTo, value);
			}
		} else {
			return null;
		}
	}
	
	protected static RuntimeException unsupportedConversionException(Class<?> class_, Object value) {
		return new RuntimeException("Unable to convert value of type "+value.getClass().getName()+" to "+class_.getName());
	}
}
