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
package step.functions.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import org.glassfish.json.OutputJsonProviderImpl;
import step.functions.io.Input;
import step.functions.io.Output;

/**
 * Factory used to create the {@link ObjectMapper} used to serialize/deserialize 
 * {@link Input} and {@link Output} instances
 *
 */
public class FunctionIOJakartaObjectMapperFactory {

	public static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		// This mapper is using a customized JsonProvider which override some classes used during deserialization
		// to replace JsonStringImpl by our own mainly for overriding the toString method.
		// The background for this is that we are abusing the usage of JsonObject (in the sense that we do not control it)
		// for the keyword output in the controller. Relying on the usage of the toString method directly or indirectly
		// via groovy string interpolation ${} which invoke the toString method.
		// This has been working as of now only because we were using a very old version of javax.json (1.0.0)
		// since 1.0.3 (2013) they changed/corrected the implementation of the toString to output a valid json string representation
		// To support Java 17 we hat do move the jakarta packages which contain this json change/fix even in the oldest version
		mapper.registerModule(new JSONPModule(new OutputJsonProviderImpl()));
		return mapper;
	}
}
