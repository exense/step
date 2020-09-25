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
package step.core.accessors.collections;

/**
 * 
 * A Map with a guaranteed max number of keys, when the threshold is exceeded,
 * values are redirected to a garbage key.
 * 
 */
public class ThresholdMap<K, V>  extends DottedKeyMap<K, V> {

	private static final long serialVersionUID = 8922169005470741941L;

	private int threshold;

	private K garbageKey;

	ThresholdMap(int threshold, K garbageKeyName){
		this.threshold = threshold;
		this.garbageKey = garbageKeyName;
	}

	@Override
	public V put(K key, V value){
		if(size() >= threshold){
			return super.put(garbageKey, value);
		}else{
			return super.put(key, value);
		}
	}

	@Override
	public V get(Object key){
		if(!containsKey(key) && size() >= threshold){
			return super.get(garbageKey);
		}else{
			return super.get(key);
		}
	}
}
