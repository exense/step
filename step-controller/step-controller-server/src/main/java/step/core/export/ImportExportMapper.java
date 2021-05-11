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
package step.core.export;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.Version;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;



public class ImportExportMapper {
	
	public static ObjectMapper getMapper(Version version) {
		ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
		if (version.compareTo(new Version(3,14,0)) >= 0) {
			mapper.addMixIn(AbstractOrganizableObject.class, MyMixin.class);
		}
		mapper.getFactory().disable(Feature.AUTO_CLOSE_TARGET);
		return mapper;
	}
	
	interface MyMixin {
		@JsonProperty("_id")
		public ObjectId getId();
		@JsonProperty("_id")
		public void setId(ObjectId _id);
	}


}
