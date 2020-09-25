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
package step.plugins.parametermanager;

import java.util.Iterator;

import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.parameter.Parameter;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionFind;
import step.core.accessors.collections.SearchOrder;

public class ParameterCollection extends Collection<Parameter> {

	public ParameterCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "parameters", Parameter.class, true);
	}

	@Override
	public CollectionFind<Parameter> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
		CollectionFind<Parameter> find = super.find(query, order, skip, limit);
		
		Iterator<Parameter> iterator = find.getIterator();
		Iterator<Parameter> filteredIterator = new Iterator<Parameter>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Parameter next() {
				Parameter next = iterator.next();
				if(next.getProtectedValue()) {
					next.setValue(ParameterServices.PROTECTED_VALUE);					
				}
				return next;
			}
			
		};
		CollectionFind<Parameter> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}

}
