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
package step.core.accessors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * With this annotation you can define attributes ({@link AbstractOrganizableObject#attributes})
 * to be added to any kind of object that ends up in a an {@link AbstractOrganizableObject}
 * 
 * This is for instance the case with Keywords:
 * </p>
 * <p>
 * <tt>
 * {@literal @}Attribute("name"="project", "value"="@system" <br>
 * public class MyKeywordLibrary extends AbstractKeyword {<br>
 * <br>
 * }
 * </tt>
 * </p>
 * <p>
 * This annotation can be added to types or methods
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {

	public String key();

	public String value();

}
