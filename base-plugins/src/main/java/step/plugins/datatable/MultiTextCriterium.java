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
package step.plugins.datatable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultiTextCriterium implements SearchQueryFactory {

	List<String> attributes;
	
	public MultiTextCriterium(List<String> attributes) {
		super();
		this.attributes = attributes;
	}
	
	public MultiTextCriterium(String... attributes) {
		super();
		this.attributes = Arrays.asList(attributes);
	}

	@Override
	public String createQuery(String attributeName, String expression) {
		StringBuilder query = new StringBuilder();
		query.append("$or:[");
		Iterator<String> it = attributes.iterator();
		while(it.hasNext()) {
			query.append("{"+it.next()+":{$regex:'"+expression+"'}}");
			if(it.hasNext()) {
				query.append(",");
			}
		}
		query.append("]");
		return query.toString();
	}

}
