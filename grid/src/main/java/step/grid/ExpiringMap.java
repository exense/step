package step.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.jvnet.hk2.internal.Closeable;

public class ExpiringMap<T,V> implements Map<T,V>, Closeable{
	
	long keepaliveTimeout;
	
	Timer keepaliveTimeoutCheckTimer;
	
	private Map<T, Wrapper> map = new HashMap<>();
	
	private class Wrapper {
		
		long lasttouch;
		
		V value;

		public Wrapper(V value) {
			super();
			this.value = value;
			lasttouch = System.currentTimeMillis();
		}
	}

	public ExpiringMap() {
		super();
		
		keepaliveTimeoutCheckTimer = new Timer();
		keepaliveTimeoutCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					keepaliveTimeoutCheck();
				} catch (Exception e) {
					
				}
			}
		}, 10000,10000);
	}
	
	private void keepaliveTimeoutCheck() {
		if(keepaliveTimeout>0) {
			synchronized (map) {
				long now = System.currentTimeMillis();
				List<T> invalidKeys = new ArrayList<>();
				for(Map.Entry<T, ExpiringMap<T, V>.Wrapper> entry:map.entrySet()) {
					if(entry.getValue().lasttouch+keepaliveTimeout<now) {
						invalidKeys.add(entry.getKey());
					}
				}
				for(T key:invalidKeys) {
					map.remove(key);				
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
		return map.get(key).value;
	}

	@Override
	public V put(T key, V value) {
		Wrapper wrapper = map.put(key, new Wrapper(value));
		return wrapper!=null?wrapper.value:null;
	}

	@Override
	public V remove(Object key) {
		Wrapper w = map.remove(key);
		return w!=null?w.value:null;
	}

	@Override
	public void putAll(Map<? extends T, ? extends V> m) {
		
	}

	@Override
	public void clear() {
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
	
	public void putOrTouch(T key, V value) {
		if(containsKey(key)) {
			touch(key);
		} else {
			put(key, value);
		}
	}
	
	public void touch(T key) {
		Wrapper v = map.get(key);
		if(v!=null) {
			v.lasttouch = System.currentTimeMillis();
		}
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}
}
