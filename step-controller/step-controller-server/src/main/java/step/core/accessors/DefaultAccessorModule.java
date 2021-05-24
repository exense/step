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

import com.fasterxml.jackson.databind.module.SimpleModule;

import ch.exense.commons.core.accessors.serialization.DottedKeyMap;
import ch.exense.commons.core.accessors.serialization.DottedMapKeyDeserializer;
import ch.exense.commons.core.accessors.serialization.DottedMapKeySerializer;

/**
 * Default Jackson module used for the serialization in the persistence layer (Jongo)
 * This module isn't used in the REST layer (Jersey) and can therefore be used to define serializers that only 
 * have to be used when persisting objects
 * 
 */
public class DefaultAccessorModule extends SimpleModule {

	private static final long serialVersionUID = 5544301456563146100L;

	public DefaultAccessorModule() {
		super();
		
		addSerializer(DottedKeyMap.class, new DottedMapKeySerializer());
		addDeserializer(DottedKeyMap.class, new DottedMapKeyDeserializer());
		
	}

}
