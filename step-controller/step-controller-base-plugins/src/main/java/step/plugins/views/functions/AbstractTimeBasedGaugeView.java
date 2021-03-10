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
package step.plugins.views.functions;

import java.util.Map;
import java.util.TreeMap;

public abstract class AbstractTimeBasedGaugeView<T> extends AbstractTimeBasedView<T> {

	@Override
	protected void mergePointToInterval(TreeMap<Long, T> newIntervals, long time, int resolution, T point) {
		//for gauge, we consider the latest value
		long interval = timeToInterval(time,resolution);
		newIntervals.put(interval, point);
	}

	@Override
	protected void initInterval(TreeMap<Long, T> intervals, long interval) {
		T entry = intervals.get(interval);
		//it's a gauge so we start from previous interval values when creating a new one
		if (entry == null) {
			Map.Entry<Long, T> longTEntry = intervals.floorEntry(interval);
			if (longTEntry != null && longTEntry.getValue() != null) {
				T previousEntry = longTEntry.getValue();
				entry = getCopy(previousEntry);
				intervals.put(interval, entry);
			}
		}
	}

	protected abstract T getCopy(T original);
	


}
