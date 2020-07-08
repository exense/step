package step.core.export;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.io.FileHelper;
import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.Entity;
import step.core.entities.EntityManager;
import step.core.entities.EntityReferencesMap;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.resources.ResourceManager;

public class ExportManager {	

	private static final Logger logger = LoggerFactory.getLogger(ExportManager.class);

	private GlobalContext context;

	public ExportManager(GlobalContext context) {
		super();
		this.context = context;
	}
	
	public void exportById(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, ObjectPredicate objectPredicate, String id, String entityType, boolean recursively, List<String> additionalEntities) throws FileNotFoundException, IOException {
		EntityReferencesMap refs = new EntityReferencesMap();
		refs.addElementTo(entityType, id);
		if (recursively) {
			context.getEntityManager().getAllEntities(entityType,id,refs);	
		}
		if (additionalEntities != null && additionalEntities.size()>0) {
			additionalEntities.forEach(e-> context.getEntityManager().getEntitiesReferences(e, objectPredicate, false, refs));
		}
		export(outputStream, objectEnricher, metadata, refs);
	}
	
	public void exportAll(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, ObjectPredicate objectPredicate, String entityType, boolean recursively, List<String> additionalEntities) throws FileNotFoundException, IOException {
		EntityReferencesMap refs = new EntityReferencesMap();
		context.getEntityManager().getEntitiesReferences(entityType, objectPredicate, recursively, refs);
		if (additionalEntities != null) {
			additionalEntities.forEach(e-> context.getEntityManager().getEntitiesReferences(e, objectPredicate, false, refs));
		}
		export(outputStream, objectEnricher, metadata, refs);
	}
	
	private void exportEntityByIds(Entity<?, ?> entity, JsonGenerator jGen, List<String> ids, ObjectEnricher objectEnricher) {
		ids.forEach(id -> {
			AbstractIdentifiableObject a = entity.getAccessor().get(id);
			if ( a == null) {
				throw new RuntimeException("Referenced entity does not exists: entity of type " + entity.getName() + " with id " + id);
			}
			objectEnricher.accept(a);
			try {
				jGen.writeObject(a);
			} catch (IOException e) {
				throw new RuntimeException("Error while exporting entity of type " + entity.getName() + " with id:" + id ,e);
			}
		});
	}

	private void export(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, EntityReferencesMap references)
			throws FileNotFoundException, IOException {
		
		ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
				
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
		//Export db content to JSON
		try (JsonGenerator jGen = mapper.getFactory().createGenerator(jsonStream, JsonEncoding.UTF8)) {
			//Header with metadata
			// pretty print
			jGen.useDefaultPrettyPrinter();
			jGen.writeStartObject();
			jGen.writeObjectField("metadata", metadata);
			//start a json array for each entity type
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
		try (ZipOutputStream zos = new ZipOutputStream(outputStream)){
			FileHelper.zipFile(zos, jsonStream, "export.json");
			//Export resources (files)
			List<String> resourceRef = references.getReferencesByType(EntityManager.resources);
			Entity<?, ?> resourceEntity = context.getEntityManager().getEntityByName(EntityManager.resources);
			if (resourceRef != null && resourceRef.size() > 0 && resourceEntity != null) {
				exportResources(zos, resourceRef, resourceEntity);
			}
		}
	}

	private void exportResources(ZipOutputStream zos, List<String> resourceRef, Entity<?, ?> resourceEntity) {
		ResourceManager resourceManager = context.get(ResourceManager.class);
		resourceRef.forEach(r-> {
			File file = resourceManager.getResourceFile(r).getResourceFile();
			try {
				FileHelper.zipFile(zos, file, resourceManager.getResourcesRootPath());
			} catch (IOException e) {
				throw new RuntimeException("Unable to add resource file to the archive",e);
			}
		});
	}
}
