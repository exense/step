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
package step.commons.datatable;

import java.util.ArrayList;
import java.util.List;

public class DataTable {
	
	private List<TableRow> rows = new ArrayList<>();

	public boolean addRow(TableRow e) {
		return rows.add(e);
	}
	
	public boolean addRows(List<TableRow> e) {
		return rows.addAll(e);
	}


	public List<TableRow> getRows() {
		return rows;
	}

}
