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
package step.datapool.sequence;

import java.util.HashMap;
import java.util.Map;

import step.datapool.DataSet;


public class IntSequenceDataPoolImpl extends DataSet<IntSequenceDataPool> {
	
	Map<String, String> params = new HashMap<>();
	
	int cursor;
	
	boolean init = true;
	
	int inc, end;
			
	public IntSequenceDataPoolImpl(IntSequenceDataPool configuration) {
		super(configuration);
	}

	@Override
	public void reset_() {
		init=true;
		cursor = configuration.getStart().get();
		inc = configuration.getInc().get();
		end = configuration.getEnd().get();
	}

	@Override
	public Object next_() {
		if(init) {
			init=false;
		} else {
			cursor+=inc;
		}
		
		if(inc>0) {
			if(cursor<end+1) {
				return cursor;
			} else {
				return null;
			}
		} else {
			if(cursor>end-1) {
				return cursor;
			} else {
				return null;
			}	
		}
	}

	@Override
	public void close() {
	}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}
