package step.migration.tasks;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.BasicDBObject;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.MongoClientSession;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessorImpl;
import step.migration.MigrationTask;

/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class MigrateExecutionParametersArtefact extends MigrationTask {

	private com.mongodb.client.MongoCollection<Document> executionCollection;
	private ExecutionAccessorImpl executionAccessor;
	private Mapper dbLayerObjectMapper;

	public MigrateExecutionParametersArtefact() {
		super(new Version(3,13,0));
	}

	@Override
	protected void setContext(GlobalContext context) {
		super.setContext(context);
		init(context.getMongoClientSession());
	}

	protected void init(MongoClientSession mongoClientSession) {
		executionCollection = mongoClientSession.getMongoDatabase().getCollection("executions");
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		
		executionAccessor = new ExecutionAccessorImpl(mongoClientSession);
	}

	@Override
	public void runUpgradeScript() {
		migrateExecutionParametersArtefact();
	}
	
	private void migrateExecutionParametersArtefact() {
		Unmarshaller unmarshaller = dbLayerObjectMapper.getUnmarshaller();
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Searching for executions be migrated...");
		executionCollection.find(BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				BasicDBObject object = (BasicDBObject) t.get("executionParameters");
				if(object != null) {
					Object artefact = object.get("artefact");
					if(artefact != null) {
						// Rename the field "artefact" to "repositoryObject"
						object.put("repositoryObject", artefact);
						// ... and save the result while ensuring integrity by unmarshalling as POJO
						Execution execution = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(object), Execution.class);
						executionAccessor.save(execution);
						successCount.incrementAndGet();
					}
				}
			} catch (Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating execution " + t, e);
			}
		});
		logger.info("Migrated "+successCount.get()+" executions successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating executions. See previous error logs for details.");
		}
	}
	
	public static void main(String[] args) throws IOException {
		ArgumentParser arguments = new ArgumentParser(args);
		Configuration configuration;
		if(arguments.hasOption("config")) {
			configuration = new Configuration(new File(arguments.getOption("config")));
		} else {
			configuration = new Configuration();
			configuration.putProperty("db.host", "localhost");
		}
		MongoClientSession mongoClientSession = new MongoClientSession(configuration);
		MigrateExecutionParametersArtefact task = new MigrateExecutionParametersArtefact();
		task.init(mongoClientSession);
		task.migrateExecutionParametersArtefact();
	}
	
	@Override
	public void runDowngradeScript() {
	}
}
