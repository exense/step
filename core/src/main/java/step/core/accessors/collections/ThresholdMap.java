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
