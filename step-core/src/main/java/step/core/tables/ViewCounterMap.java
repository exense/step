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

public class ViewCounterMap  extends ThresholdMap<String, Integer> {

	private static final long serialVersionUID = -5842315205753972877L;
	
	public ViewCounterMap(){
		super(500, "Other");
	}
	
	public ViewCounterMap(int threshold, String defaultKey){
		super(threshold, defaultKey);
	}
	
	public void incrementForKey(String key){
		Integer current = get(key);
		if(current == null){
			put(key, 1);
		}
		else{
			put(key, current + 1);
		}
	}
	
	public void decrementForKey(String key){
		Integer current = get(key);
		if(current != null){
			put(key, current - 1);
		}
	}
}
