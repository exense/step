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
package step.commons.iterators;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SkipLimitIterator<T> implements Iterator<T> {

	private final int batchSize;
	
	private final SkipLimitProvider<T> provider;

	private Iterator<T> currentBatchIt = null;
	private int currentBatchCount = 0;
	private int currentSkip = 0;
	
	private T next = null;
	
	public SkipLimitIterator(SkipLimitProvider<T> provider) {
		this(provider, 1000);
	}
	
	public SkipLimitIterator(SkipLimitProvider<T> provider, int batchSize) {
		super();
		this.provider = provider;
		this.batchSize = batchSize;
		getNextBatch();
		preloadNextElement();
	}

	protected void preloadNextElement() {
		if(currentBatchIt.hasNext()) {
			currentBatchCount++;
			next = currentBatchIt.next();
		} else {
			if(currentBatchCount==batchSize) {
				getNextBatch();
				preloadNextElement();
			} else {
				next = null;
			}
		}
	}

	protected void getNextBatch() {
		List<T> batch = provider.getBatch(currentSkip, batchSize);
		if(batch.size()>batchSize) {
			throw new RuntimeException("The size of the batch returned by the SkipLimitProvider is higher than the specified batch size. Expected size was "
											+batchSize+". Actual batch size was "+batch.size());
		}
		currentBatchIt = batch.iterator();
		currentBatchCount = 0;
		currentSkip+=batchSize;
	}
	
	@Override
	public boolean hasNext() {
		return next!=null;
	}

	@Override
	public T next() {
		T result = next;
		
		if (next==null) {
			throw new NoSuchElementException();
		}
			
		preloadNextElement();
		return result;
	}
}
