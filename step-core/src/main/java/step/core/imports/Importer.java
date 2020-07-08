package step.core.imports;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.entities.Entity;

public interface Importer<A extends AbstractIdentifiableObject, T extends CRUDAccessor<A>> {
	
	public void init(Entity<A, T> entity);
	
	public A importOne(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper, Map<String, String> references)  throws JsonParseException, JsonMappingException, IOException;
	
	public void importMany(ImportConfiguration importConfig, ObjectMapper mapper) throws IOException; 
}
