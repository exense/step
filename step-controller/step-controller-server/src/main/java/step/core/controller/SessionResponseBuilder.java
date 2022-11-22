package step.core.controller;

import step.framework.server.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SessionResponseBuilder {

	private final List<Function<Session, Map<String, Object>>> hooks = new ArrayList<>();

	public boolean registerHook(Function<Session, Map<String, Object>> sessionMapFunction) {
		return hooks.add(sessionMapFunction);
	}

	public Map<String, Object> build(Session session) {
		Map<String, Object> result = new HashMap<>();
		hooks.forEach(h -> {
			Map<String, Object> hookProperties = h.apply(session);
			result.putAll(hookProperties);
		});
		return result;
	}
}
