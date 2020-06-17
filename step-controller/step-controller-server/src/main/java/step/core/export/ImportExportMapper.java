package step.core.export;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.CRUDAccessor;
import step.core.deployment.JacksonMapperProvider;
import step.core.imports.Importer;
import step.core.imports.PlanImporter;

public class ImportExportMapper {
	
	public enum EntitityType {
		plans,
		parameters
	}
	
	public static Map<String,Class<? extends Importer>> importers = new HashMap<String,Class<? extends Importer>>();
	
	
	static {
		importers.put("plans",PlanImporter.class);
	}
	
	public static Importer createImporter(String entityType, GlobalContext c) throws InstantiationException, IllegalAccessException {
		Importer importer = null;
		if (importers.containsKey(entityType)) {
			importer = importers.get(entityType).newInstance();
			importer.init(c,entityType);
		}
		return importer;
	}
	
	public static CRUDAccessor<? extends AbstractIdentifiableObject> getAccessorByName(GlobalContext context, String entityName) {
		EntitityType type = Enum.valueOf(EntitityType.class, entityName);
		switch (type) {
			case plans: return context.getPlanAccessor(); 
			default: return null; 
		}
	}
	
	public static ObjectMapper getMapper(Version version) {
		ObjectMapper mapper = JacksonMapperProvider.createMapper();
		if (version.compareTo(new Version(3,14,0)) >= 0) {
			mapper.addMixIn(AbstractOrganizableObject.class, MyMixin.class);
		}
		mapper.getFactory().disable(Feature.AUTO_CLOSE_TARGET);
		return mapper;
	}
	
	interface MyMixin {
		@JsonProperty("_id")
		public ObjectId getId();
		@JsonProperty("_id")
		public void setId(ObjectId _id);
	}


}
