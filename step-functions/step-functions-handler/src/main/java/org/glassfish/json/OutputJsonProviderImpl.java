package org.glassfish.json;

import jakarta.json.JsonBuilderFactory;
import org.glassfish.json.api.BufferPool;

import java.util.Map;

public class OutputJsonProviderImpl extends JsonProviderImpl {

	@Override
	public JsonBuilderFactory createBuilderFactory(Map<String, ?> config) {
		BufferPool pool = new BufferPoolImpl();
		boolean rejectDuplicateKeys = false;
		if (config != null) {
			if (config.containsKey(BufferPool.class.getName())) {
				pool = (BufferPool) config.get(BufferPool.class.getName());
			}
			rejectDuplicateKeys = JsonProviderImpl.isRejectDuplicateKeysEnabled(config);
		}
		return new OutputJsonBuilderFactoryImpl(pool, rejectDuplicateKeys);
	}


}
