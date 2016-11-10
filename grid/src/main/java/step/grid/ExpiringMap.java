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
package step.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.jvnet.hk2.internal.Closeable;

public class ExpiringMap<T,V> implements Map<T,V>, Closeable{
	
	long keepaliveTimeout;
	
	Timer keepaliveTimeoutCheckTimer;
	
	private ConcurrentHashMap<T, Wrapper> map = new ConcurrentHashMap<>();
	
	private class Wrapper {
		
		long lasttouch;
		
		V value;

		public Wrapper(V value) {
			super();
			this.value = value;
			lasttouch = System.currentTimeMillis();
		}
	}

	public ExpiringMap(long keepaliveTimeout) {
		this(keepaliveTimeout, keepaliveTimeout/10);
	}
	
	public ExpiringMap(long keepaliveTimeout, long checkIntervalMs) {
		super();
		
		this.keepaliveTimeout = keepaliveTimeout;
		
		keepaliveTimeoutCheckTimer = new Timer();
		keepaliveTimeoutCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					keepaliveTimeoutCheck();
				} catch (Exception e) {
					
				}
			}
		}, checkIntervalMs,checkIntervalMs);
	}
	
	private void keepaliveTimeoutCheck() {
		if(keepaliveTimeout>0) {
			long now = System.currentTimeMillis();			
			Set<Map.Entry<T, ExpiringMap<T, V>.Wrapper>> set = map.entrySet();
			for(Map.Entry<T, ExpiringMap<T, V>.Wrapper> entry:set) {
				if(entry.getValue().lasttouch+keepaliveTimeout<now) {
					set.remove(entry);
				}
			}
		}
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for(Wrapper w:map.values()) {
			if(w.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		Wrapper w = map.get(key);
		return w!=null?w.value:null;
	}

	@Override
	public synchronized V put(T key, V value) {
		Wrapper wrapper = map.put(key, new Wrapper(value));
		return wrapper!=null?wrapper.value:null;
	}

	@Override
	public synchronized V remove(Object key) {
		Wrapper w = map.remove(key);
		return w!=null?w.value:null;
	}

	@Override
	public void putAll(Map<? extends T, ? extends V> m) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public synchronized void clear() {
		map.clear();
	}

	@Override
	public Set<T> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		List<V> values = new ArrayList<>();
		for(ExpiringMap<T, V>.Wrapper entry:map.values()) {
			values.add(entry.value);
		}
		return values;
	}

	@Override
	public Set<java.util.Map.Entry<T, V>> entrySet() {
		Set<java.util.Map.Entry<T, V>> result = new HashSet<>();
		for(java.util.Map.Entry<T, ExpiringMap<T, V>.Wrapper> e:map.entrySet()) {
			result.add(new Entry(e.getKey(), e.getValue().value));
		}
		return result;
	}
	
	private class Entry implements java.util.Map.Entry<T, V> {
		T key;
		
		V value;
		
		public Entry(T key, V value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public T getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new RuntimeException("Not implemented");
		}
	}

	@Override
	public boolean close() {
		keepaliveTimeoutCheckTimer.cancel();
		return true;
	}
	
	public synchronized void putOrTouch(T key, V value) {
		if(containsKey(key)) {
			touch(key);
		} else {
			put(key, value);
		}
	}
	
	public synchronized void touch(T key) {
		Wrapper v = map.get(key);
		if(v!=null) {
			v.lasttouch = System.currentTimeMillis();
		}
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
