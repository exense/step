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
package step.core.tables;

import java.util.Iterator;

public class TableFindResult<T> {

	long recordsTotal;
	
	long recordsFiltered;
	
	public TableFindResult(long recordsTotal, long recordsFiltered,
			Iterator<T> iterator) {
		super();
		this.recordsTotal = recordsTotal;
		this.recordsFiltered = recordsFiltered;
		this.iterator = iterator;
	}

	Iterator<T> iterator;

	public long getRecordsTotal() {
		return recordsTotal;
	}

	public long getRecordsFiltered() {
		return recordsFiltered;
	}

	public Iterator<T> getIterator() {
		return iterator;
	}
}
