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
package step.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IntegerSequenceIterator implements Iterator<Integer> {

	AtomicInteger counter;
	int end;
	int increment;
	
	public IntegerSequenceIterator(int start, int end, int increment) {
		counter = new AtomicInteger(start);
		this.end = end;
		this.increment = increment;
	}
	@Override
	public boolean hasNext() {
		//never called from the worker run method
		return (counter.get()<=end);
	}

	@Override
	public Integer next() {
		//must return null if reached the end
		Integer value = counter.getAndAdd(increment);
		return (value<=end) ? value : null;
	}
}
