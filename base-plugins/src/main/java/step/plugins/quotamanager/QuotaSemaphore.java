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
package step.plugins.quotamanager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

class QuotaSemaphore extends Semaphore {
	private static final long serialVersionUID = -3808791893658360762L;
	
	private final Object lockCounterObject = new Object();

	AtomicInteger load = new AtomicInteger(0);
	AtomicInteger peak = new AtomicInteger(0);
	
	public QuotaSemaphore(int permits) {
		super(permits);
	}

	public QuotaSemaphore(int permits, boolean fair) {
		super(permits, fair);
	}
	
	@Override
	public void acquire() throws InterruptedException {
		super.acquire();
	}

	public void decrementLoad() {
		synchronized (lockCounterObject) {
			load.decrementAndGet();
			load.set(Math.max(0, load.intValue()));
			peak.set(Math.max(peak.intValue(), load.intValue()));
		}
	}

	public int getLoad() {
		return load.intValue();
	}

	public int getPeak() {
		return peak.intValue();
	}

	public void incrementLoad() {
		synchronized (lockCounterObject) {
			load.incrementAndGet();
			peak.set(Math.max(peak.intValue(), load.intValue()));
		}
		
	}

	@Override
	public void release() {
		super.release();
	}
}