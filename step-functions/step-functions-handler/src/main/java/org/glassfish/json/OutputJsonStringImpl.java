package org.glassfish.json;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public class OutputJsonStringImpl implements JsonString {

	private final String value;

	OutputJsonStringImpl(String value) {
		this.value = value;
	}

	@Override
	public String getString() {
		return value;
	}

	@Override
	public CharSequence getChars() {
		return value;
	}

	@Override
	public JsonValue.ValueType getValueType() {
		return JsonValue.ValueType.STRING;
	}

	@Override
	public int hashCode() {
		return getString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (!(obj instanceof JsonString)) {
			return false;
		}
		JsonString other = (JsonString)obj;
		return getString().equals(other.getString());
	}

	@Override
	public String toString() {
		return value;
	}
}
