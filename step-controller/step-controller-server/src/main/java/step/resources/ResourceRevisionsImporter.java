package step.resources;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.imports.GenericDBImporter;
import step.core.imports.ImportConfiguration;


public class ResourceRevisionsImporter extends GenericDBImporter<ResourceRevision, ResourceRevisionAccessor> {

	public ResourceRevisionsImporter(GlobalContext context) {
		super(context);
	}

	@Override
	public ResourceRevision importOne(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper,
			Map<String, String> references) throws JsonParseException, JsonMappingException, IOException {
		ResourceRevision resourceRevision = mapper.readValue(jParser, entity.getEntityClass());
		importConfig.getObjectEnricher().accept(resourceRevision);
		resourceRevision = importConfig.getLocalResourceMgr().saveResourceRevision(resourceRevision);
		return resourceRevision;
	}
}
