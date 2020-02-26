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
package step.core.accessors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class DateRangeCriterium implements SearchQueryFactory {
	
	private SimpleDateFormat DATE_FORMAT;
	
	public DateRangeCriterium(String dateFormat) {
		super();
		
		DATE_FORMAT = new SimpleDateFormat(dateFormat);
	}

	@Override
	public Bson createQuery(String attributeName, String expression) {

		try {
			Date from;
			synchronized (DATE_FORMAT) {
				from = DATE_FORMAT.parse(expression);				
			}
			
			Calendar c = Calendar.getInstance();
			c.setTime(from);
			c.add(Calendar.DATE, 1);
			
			Date to = c.getTime();
			
			return Filters.and(Filters.lt(attributeName, to.getTime()), Filters.gte(attributeName, from.getTime()));
		} catch (ParseException e) {
			return null;
		}
	}

}
