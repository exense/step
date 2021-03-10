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

import java.util.HashMap;
import java.util.Map;

public class ThreadGroupStatisticsEntry {

	int count;

	Map<String, Statistics> byThreadGroupName = new HashMap<>();

	public int getCount() {
		return count;
	}


	public Map<String, Statistics> getByThreadGroupName() {
		return byThreadGroupName;
	}

	public ThreadGroupStatisticsEntry deepCopy() {
		ThreadGroupStatisticsEntry copy = new ThreadGroupStatisticsEntry();
		copy.count = this.count;
		byThreadGroupName.forEach((k,v)->{
			Statistics copyStats = new Statistics(v.count);
			copy.byThreadGroupName.put(k,copyStats);
		});
		return copy;
	}

	public static class Statistics {
		int count;


		public Statistics() {
			super();
		}

		public Statistics(int count) {
			super();
			this.count = count;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

}
