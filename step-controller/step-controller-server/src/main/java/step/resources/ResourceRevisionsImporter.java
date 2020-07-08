package step.resources;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.Version;
import step.core.imports.GenericDBImporter;
import step.core.objectenricher.ObjectEnricher;


public class ResourceRevisionsImporter extends GenericDBImporter<ResourceRevision, ResourceRevisionAccessor> {

	public ResourceRevisionsImporter(GlobalContext context) {
		super(context);
	}

	@Override
	public ResourceRevision importOne(JsonParser jParser, ObjectMapper mapper, ObjectEnricher objectEnricher, Version version, 
			Map<String, String> references, ResourceManager localResourceMgr, boolean overwrite) throws JsonParseException, JsonMappingException, IOException {
		ResourceRevision resourceRevision = mapper.readValue(jParser, entity.getEntityClass());
		objectEnricher.accept(resourceRevision);
		resourceRevision = localResourceMgr.saveResourceRevision(resourceRevision);
		return resourceRevision;
	}
}
