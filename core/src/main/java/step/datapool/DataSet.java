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
package step.datapool;

import step.core.execution.ExecutionContext;

public abstract class DataSet<T> {
	
	protected final T configuration;
	
	protected ExecutionContext context;
	
	public DataSet(T configuration) {
		super();
		this.configuration = configuration;
	}

	public abstract void init();

	public abstract void reset();
	
	public abstract void close();
	
	public final synchronized DataPoolRow next() {
		Object nextValue = next_();
		return nextValue!=null?new DataPoolRow(nextValue):null;
	}
	
	public abstract Object next_();
	
	public abstract void addRow(Object row);
	
	public void save() {}

	protected void setContext(ExecutionContext context) {
		this.context = context;
	};
}
