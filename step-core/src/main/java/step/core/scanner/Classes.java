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
package step.core.scanner;

import java.util.function.Function;

public class Classes {

	public static Function<String, Class<?>> loadWith(ClassLoader loader) {
		return classname->{
			try {
				return loader.loadClass(classname);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to load class "+classname, e);
			}
		};
	}
	
	public static <T> Function<Class<?>,T> newInstanceAs(Class<T> loadAs) {
		return t->newInstanceAs(t, loadAs);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newInstanceAs(Class<?> class_, Class<T> loadAsClass) {
		Object newInstance = newInstance(class_);
		if(loadAsClass.isInstance(newInstance)) {
			return (T) newInstance;
		} else {
			throw new RuntimeException("The class "+class_.getName()+" is not an instance of "+loadAsClass.getName());
		}
	}

	public static <T> T newInstance(Class<T> class_) {
		T newInstance;
		try {
			newInstance = class_.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Error while instanciating class "+class_, e);
		}
		return newInstance;
	}
}
