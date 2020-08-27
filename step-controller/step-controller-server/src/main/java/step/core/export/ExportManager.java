package step.core.export;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
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
import step.resources.ResourceManager;

public class ExportManager {
	
	private static Logger logger = LoggerFactory.getLogger(ExportManager.class);

	private GlobalContext context;

	public ExportManager(GlobalContext context) {
		super();
		this.context = context;
	}
	
	public Set<String> exportById(ExportConfiguration exportConfig, String id) throws FileNotFoundException, IOException {
		EntityReferencesMap refs = new EntityReferencesMap();
		refs.addElementTo(exportConfig.entityType, id);
		if (exportConfig.recursively) {
			context.getEntityManager().getAllEntities(exportConfig.entityType, id, refs);	
		}
		if (exportConfig.additionalEntities != null && exportConfig.additionalEntities.size()>0) {
			exportConfig.additionalEntities.forEach(e-> 
				context.getEntityManager().getEntitiesReferences(e, exportConfig.objectPredicate, false, refs)
			);
		}
		export(exportConfig, refs);
		return refs.getRefNotFoundWarnings();
	}
	
	public Set<String> exportAll(ExportConfiguration exportConfig) throws FileNotFoundException, IOException {
		EntityReferencesMap refs = new EntityReferencesMap();
		context.getEntityManager().getEntitiesReferences(exportConfig.entityType, exportConfig.objectPredicate, exportConfig.recursively, refs);
		if (exportConfig.additionalEntities != null) {
			exportConfig.additionalEntities.forEach(e-> context.getEntityManager().getEntitiesReferences(e, exportConfig.objectPredicate, false, refs));
		}
		export(exportConfig, refs);
		return refs.getRefNotFoundWarnings();
	}
	
	private void exportEntityByIds(String entityName, JsonGenerator jGen, EntityReferencesMap references, ObjectEnricher objectEnricher) {
		Entity<?, ?> entity = context.getEntityManager().getEntityByName(entityName);
		if (entity == null ) {
			throw new RuntimeException("Entity of type " + entityName + " is not supported");
		}
		references.getReferencesByType(entityName).forEach(id -> {
			AbstractIdentifiableObject a = entity.getAccessor().get(id);
			if ( a == null) {
				logger.warn("Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
				references.addReferenceNotFoundWarning("Referenced entity with id '" + id + "' and type '" + entityName + "' is missing");
			} else {
				objectEnricher.accept(a);
				try {
					jGen.writeObject(a);
				} catch (IOException e) {
					throw new RuntimeException("Error while exporting entity of type " + entity.getName() + " with id:" + id ,e);
				}
			}
		});
	}

	private void export(ExportConfiguration exportConfig, EntityReferencesMap references)
			throws FileNotFoundException, IOException {
		
		ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
				
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
		//Export db content to JSON
		try (JsonGenerator jGen = mapper.getFactory().createGenerator(jsonStream, JsonEncoding.UTF8)) {
			//Header with metadata
			// pretty print
			jGen.useDefaultPrettyPrinter();
			jGen.writeStartObject();
			jGen.writeObjectField("metadata", exportConfig.metadata);
			//start a json array for each entity type
			references.getTypes().forEach(e-> {
				try {
					jGen.writeArrayFieldStart(e);			
					exportEntityByIds(e, jGen, references, exportConfig.objectEnricher);
					jGen.writeEndArray();	
				} catch (IOException e1) {
					throw new RuntimeException("Error while exporting entity of type " + e,e1);
				}
			});
			jGen.writeEndObject();//end export object
		}
		try (ZipOutputStream zos = new ZipOutputStream(exportConfig.outputStream)){
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
		ResourceManager resourceManager = context.getResourceManager();
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
