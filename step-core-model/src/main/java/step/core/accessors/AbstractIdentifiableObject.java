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
package step.core.accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.persistence.Id;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class is the parent class of all objects that have to be identified
 * uniquely for persistence purposes for instance
 *
 */
public class AbstractIdentifiableObject {

	public static final String ID = "id";
	
	private ObjectId _id;
	
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
	@Id
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
	
	@SuppressWarnings("unchecked")
	public <T> T computeCustomFieldIfAbsent(String key, Function<? super String, T> mappingFunction) {
		if(customFields!=null) {
			return (T) customFields.computeIfAbsent(key, mappingFunction);
		} else {
			return mappingFunction.apply(key);
		}
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
