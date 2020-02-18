package step.core.accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class is the parent class of all objects that have to be identified
 * uniquely for persistence purposes for instance
 *
 */
public class AbstractIdentifiableObject {

	protected ObjectId _id;
	
	@JsonSerialize(using = MapSerializer.class)
	@JsonDeserialize(using = MapDeserializer.class) 
	protected Map<String, Object> customFields;
	
	public AbstractIdentifiableObject() {
		super();
		_id = new ObjectId();
	}

	/**
	 * @return the unique ID of this object
	 */
	public ObjectId getId() {
		return _id;
	}
	
	/**
	 * @param _id the unique ID of this object
	 */
	public void setId(ObjectId _id) {
		this._id = _id;
	}

	public Map<String, Object> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Map<String, Object> customFields) {
		this.customFields = customFields;
	}
	
	public Object getCustomField(String key) {
		if(customFields!=null) {
			return customFields.get(key);
		} else {
			return null;
		}
	}
	
	public Object computeCustomFieldIfAbsent(String key, Function<? super String, ? extends Object> mappingFunction) {
		return customFields.computeIfAbsent(key, mappingFunction);
	}

	@SuppressWarnings("unchecked")
	public <T> T getCustomField(String key, Class<T> valueClass) {
		Object value = getCustomField(key);
		if(value != null) {
			if(valueClass.isInstance(value)) {
				return (T) value;
			} else {
				throw new IllegalArgumentException("The value of the field "+key+" isn't an instance of "+valueClass.getCanonicalName());
			}
		} else {
			return null;
		}
	}

	public synchronized void addCustomField(String key, Object value) {
		if(customFields==null) {
			customFields = new HashMap<>();
		}
		customFields.put(key, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractIdentifiableObject other = (AbstractIdentifiableObject) obj;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		return true;
	}
}
