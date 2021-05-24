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

import java.util.Map;
import java.util.function.Consumer;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;

/**
 * Instances of this class are responsible for the enrichment of 
 * entities with context parameters. Enrichment refers to the process of
 * adding context parameters to the entites that are subject to it 
 * (like {@link AbstractOrganizableObject} for instance) 
 */
public interface ObjectEnricher extends Consumer<Object> {
	
	public Map<String, String> getAdditionalAttributes();
}
