package org.glassfish.json;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.glassfish.json.api.BufferPool;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class OutputJsonBuilderFactoryImpl implements JsonBuilderFactory {
	private final Map<String, ?> config;
	private final BufferPool bufferPool;
	private final boolean rejectDuplicateKeys;

	OutputJsonBuilderFactoryImpl(BufferPool bufferPool, boolean rejectDuplicateKeys) {
		this.config = Collections.emptyMap();
		this.bufferPool = bufferPool;
		this.rejectDuplicateKeys = rejectDuplicateKeys;
	}

	@Override
	public JsonObjectBuilder createObjectBuilder() {
		return new OutputJsonObjectBuilderImpl(bufferPool, rejectDuplicateKeys);
	}

	@Override
	public JsonObjectBuilder createObjectBuilder(JsonObject object) {
		return new OutputJsonObjectBuilderImpl(object, bufferPool, rejectDuplicateKeys);
	}

	@Override
	public JsonObjectBuilder createObjectBuilder(Map<String, Object> object) {
		return new OutputJsonObjectBuilderImpl(object, bufferPool, rejectDuplicateKeys);
	}

	@Override
	public JsonArrayBuilder createArrayBuilder() {
		return new OutputJsonArrayBuilderImpl(bufferPool);
	}

	@Override
	public JsonArrayBuilder createArrayBuilder(JsonArray array) {
		return new OutputJsonArrayBuilderImpl(array, bufferPool);
	}

	@Override
	public JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
		return new OutputJsonArrayBuilderImpl(collection, bufferPool);
	}

	@Override
	public Map<String, ?> getConfigInUse() {
		return config;
	}

}
