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
package step.core.entities;

import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;

public interface DependencyTreeVisitorHook {

	/**
	 * This hook is called for each visited entity when traversing an entity tree with
	 * {@link EntityDependencyTreeVisitor}.<br>
	 * <br>
	 * 
	 * This hook can be used to handle custom dependencies that cannot be handled by
	 * the annotation {@link EntityReference}: to handle a custom dependency the
	 * hook should call the method
	 * {@link EntityTreeVisitorContext#visitEntity(String, String)} of the context
	 * object
	 * 
	 * @param entity  the entity that has been visited
	 * @param context
	 */
	void onVisitEntity(Object entity, EntityTreeVisitorContext context);

}
