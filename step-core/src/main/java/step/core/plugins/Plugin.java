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
package step.core.plugins;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {
	
	/**
	 * @return the list of plugins this plugin is depending on
	 */
	 Class<?>[] dependencies() default {};
	 
	/**
	 * @return the list of plugins before which this plugin should be executed. 
	 * This has the inverse meaning of the "dependencies" attribute. In some cases
	 * we prefer specify that "A has to be run before B" instead of "B is depending on A".
	 * This is for example the case when B doesn't know A
	 */
	Class<?>[] runsBefore() default {};	 
}
