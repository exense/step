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
package step.core.objectenricher;

import step.core.AbstractContext;

/**
 * An {@link ObjectHook} is a factory for
 * {@link ObjectFilter} and {@link ObjectEnricher}
 *
 */
public interface ObjectHook {

	ObjectFilter getObjectFilter(AbstractContext context);
	
	ObjectEnricher getObjectEnricher(AbstractContext context);

	/**
	 * Rebuilds an {@link AbstractContext} based on an object that has been
	 * previously enriched with an {@link ObjectEnricher} provided by this class
	 * 
	 * @param context the context to be recreated
	 * @param object the object to base the context reconstruction on
	 * @throws Exception
	 */
	void rebuildContext(AbstractContext context, EnricheableObject object) throws Exception;
	
	/**
	 * Check if the provided object is acceptable in the provided context
	 * 
	 * @param context
	 * @param object
	 * @return true if the provided object belongs to the provided context or
	 *         doesn't belong to any context
	 */
	boolean isObjectAcceptableInContext(AbstractContext context, EnricheableObject object);
}
