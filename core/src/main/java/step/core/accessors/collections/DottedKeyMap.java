package step.core.accessors.collections;

import java.util.HashMap;

/**
 * 
 * A special Map that is serialized by {@link DottedMapKeySerializer}
 * when persisted in the DB. This serializer supports the persistence of keys
 * that contain "." and "$" which are normally not allowed as key by Mongo.
 * 
 * 
 */
public class DottedKeyMap<K, V>  extends HashMap<K, V> {

	private static final long serialVersionUID = 8922169005470741941L;

}
