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
package step.plugins.datatable.formatters;

import java.util.Collection;
import java.util.Iterator;

import com.mongodb.DBObject;

public class ArrayFormatter implements Formatter {

	@Override
	public String format(Object value, DBObject row) {
		if(value!=null && value instanceof Collection) {
			StringBuilder b = new StringBuilder();
			Iterator<?> i = ((Collection<?>)value).iterator();
			while(i.hasNext()) {
				b.append(i.next().toString());
				if(i.hasNext()) {
					b.append(";");
				}
			}
			return b.toString();
		} else {
			return null;
		}
			
	}

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("Not implemented");
	}

}
