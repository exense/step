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

import java.util.concurrent.Semaphore;

public class DataPoolRow {
	
	private final Object value;
	
	private final Semaphore commitLock = new Semaphore(0);

	public DataPoolRow(Object value) {
		super();
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public String toString(){return value.toString();}
	
	/**
	 * This method has to be called by the user when {@link DataSet#isRowCommitEnabled} is set to true
	 * at the end of each iteration
	 */
	public void commit() {
		commitLock.release();
	}
	
	/**
	 * Wait for the row to be committed
	 * @throws InterruptedException
	 */
	public void waitForCommit() throws InterruptedException {
		commitLock.acquire();
	}
	
}
