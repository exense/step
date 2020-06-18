package step.core.export;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.Version;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.JacksonMapperProvider;



public class ImportExportMapper {
	
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
