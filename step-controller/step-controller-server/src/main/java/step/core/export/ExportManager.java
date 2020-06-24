package step.core.export;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.entities.EntityReferencesMap;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;

public class ExportManager {	

	private static final Logger logger = LoggerFactory.getLogger(ExportManager.class);

	private GlobalContext context;

	public ExportManager(GlobalContext context) {
		super();
		this.context = context;
	}
	
	public void exportById(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, String id, String entityType) throws FileNotFoundException, IOException {
		EntityReferencesMap refs = new EntityReferencesMap();
		refs.addElementTo(entityType, id);
		export(outputStream, objectEnricher, metadata, refs);
	}
	
	public void exportAll(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, ObjectPredicate objectPredicate, String entityType) throws FileNotFoundException, IOException {
		Entity<?, ?> entity = context.getEntityManager().getEntityByName(entityType);
		if (entity == null ) {
			throw new RuntimeException("Entity of type " + entityType + " is not supported");
		}
		EntityReferencesMap refs = new EntityReferencesMap();
		entity.getAccessor().getAll().forEachRemaining(a -> {
			if (objectPredicate.test(a)) {
				refs.addElementTo(entityType, a.getId().toHexString());
			}
		});
		export(outputStream, objectEnricher, metadata, refs);
	}
	
	public void exportPlanRecursively(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, String id)
			throws FileNotFoundException, IOException {
		EntityReferencesMap references = new EntityReferencesMap();
		references.addElementTo(EntityManager.plans, id);
		context.getEntityManager().getAllEntities(EntityManager.plans,id,references);
		export(outputStream, objectEnricher, metadata, references);
	}
	
	private void exportEntityByIds(Entity<?, ?> entity, JsonGenerator jGen, List<String> ids, ObjectEnricher objectEnricher) {
		ids.forEach(id -> {
			AbstractIdentifiableObject a = entity.getAccessor().get(id);
			objectEnricher.accept(a);
			try {
				jGen.writeObject(a);
			} catch (IOException e) {
				throw new RuntimeException("Error while exporting entity of type " + entity.getName() + " with id:" + id ,e);
			}
		});
	}
	
	/*private void exportEntityByPredicate(Entity<?, ?> entity, JsonGenerator jGen, ObjectPredicate objectPredicate, ObjectEnricher objectEnricher) {
		entity.getAccessor().getAll().forEachRemaining(a -> {
			if (objectPredicate.test(a)) {
				try {
					objectEnricher.accept(a);
					jGen.writeObject(a);
				} catch (Exception e) {
					logger.error("Error while exporting entity of type " + entity.getName() + " with id:" + a.getId().toString(), e);
				}
			}
		});
	}*/

	private void export(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, EntityReferencesMap references)
			throws FileNotFoundException, IOException {
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
		try (JsonGenerator jGen = mapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {
			//Header with metadata
			// pretty print
			jGen.useDefaultPrettyPrinter();
			jGen.writeStartObject();
			jGen.writeObjectField("metadata", metadata);
			//start a json array for each entity type
			//TODO sort entity types in import order (resources first,keywords and plans (order of plans might import as well) )
			references.getTypes().forEach(e-> {
				try {
					jGen.writeArrayFieldStart(e);			
					Entity<?, ?> entity = context.getEntityManager().getEntityByName(e);
					if (entity == null ) {
						throw new RuntimeException("Entity of type " + e + " is not supported");
					}
					exportEntityByIds(entity, jGen, references.getReferencesByType(e), objectEnricher);
					jGen.writeEndArray();	
				} catch (IOException e1) {
					throw new RuntimeException("Error while exporting entity of type " + e,e1);
				}
			});
			jGen.writeEndObject();//end export object
		} 
	}
	
	

	
}
