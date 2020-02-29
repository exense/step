package step.core.accessors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

public class AccessorLayerJacksonMapperProvider {

	public static List<Module> modules = new ArrayList<>();
	
	static {
		modules.add(new JSR353Module());
		modules.add(new JsonOrgModule());
		modules.add(new DefaultAccessorModule());
	}

	public static List<Module> getModules() {
		return modules;
	}

	public static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		modules.forEach(m->objectMapper.registerModule(m));
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}
	
}
