package step.core.export;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.entities.Entity;
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
		export(outputStream, objectEnricher, metadata, id, null, entityType);
	}
	
	public void exportAll(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, ObjectPredicate objectPredicate, String entityType) throws FileNotFoundException, IOException {
		export(outputStream, objectEnricher, metadata, null, objectPredicate, entityType);
	}

	private void export(OutputStream outputStream, ObjectEnricher objectEnricher, Map<String, String> metadata, String id, ObjectPredicate objectPredicate, String entityType)
			throws FileNotFoundException, IOException {
		ObjectMapper mapper = ImportExportMapper.getMapper(context.getCurrentVersion());
		try (JsonGenerator jGen = mapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {
			Entity<?, ?> entity = context.getEntityManager().getEntityByName(entityType);
			if (entity == null ) {
				throw new RuntimeException("Entity of type " + entityType + " is not supported");
			}
			CRUDAccessor<? extends AbstractIdentifiableObject> accessor = entity.getAccessor();
			// pretty print
			jGen.useDefaultPrettyPrinter();
			jGen.writeStartObject();
			jGen.writeObjectField("metadata", metadata);
			jGen.writeArrayFieldStart(entityType);
			//entity by id
			if (id != null) {
				AbstractIdentifiableObject a = accessor.get(id);
				objectEnricher.accept(a);
				jGen.writeObject(a);
			} else if (objectPredicate != null) {
				accessor.getAll().forEachRemaining(a -> {
					if (objectPredicate.test(a)) {
						try {
							objectEnricher.accept(a);
							jGen.writeObject(a);
						} catch (Exception e) {
							logger.error("Error while exporting entity " + a.getId().toString(), e);
						}
					}
				});
			} else {
				logger.error("Error while exporting entity, not id or objectPredicate provided.");
			}
			jGen.writeEndArray();
			jGen.writeEndObject();//end export object
		} catch (Exception e) {
			logger.error("Error while exporting artefact with id " + id, e);
		}
	}

	
}
