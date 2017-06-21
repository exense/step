/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.adaptergrid;

import java.util.Iterator;
import java.util.Map;

import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.functions.Function;
import step.functions.FunctionRepository;

public class FunctionRepositoryImpl implements FunctionRepository {

	private MongoCollection functions;
	
	private static JsonProvider jsonProvider = JsonProvider.provider();
		
	public FunctionRepositoryImpl(MongoCollection functions) {
		super();
		this.functions = functions;
	}

	@Override
	public Function getFunctionById(String id) {
		return functions.findOne(new ObjectId(id)).as(Function.class);
	}

	@Override
	public Function getFunctionByAttributes(Map<String, String> attributes) {		
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		for(String key:attributes.keySet()) {
			builder.add("attributes."+key, attributes.get(key));
		}

		String query = builder.build().toString();
		
		Iterator<Function> it = functions.find(query).as(Function.class).iterator();
		if(it.hasNext()) {
			Function function = it.next();
			
			if(it.hasNext()) {
				// TODO warning
			} 
			
			return function;
		} else {
			throw new RuntimeException("Unable to find function with attributes "+attributes.toString());
		}
	}

	@Override
	public void addFunction(Function function) {
		functions.save(function);
	}

	@Override
	public void deleteFunction(String functionId) {
		functions.remove(new ObjectId(functionId));
	}

}
