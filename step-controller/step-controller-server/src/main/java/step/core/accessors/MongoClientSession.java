package step.core.accessors;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.Configuration;

public class MongoClientSession implements Closeable {

	protected MongoClient mongoClient;
	
	protected String db;
	
	protected Configuration configuration;

	public MongoClientSession(Configuration configuration) {
		super();
		this.configuration = configuration;
		
		initMongoClient();
	}
	
	protected void initMongoClient() {
		String host = configuration.getProperty("db.host");
		Integer port = configuration.getPropertyAsInteger("db.port",27017);
		String user = configuration.getProperty("db.username");
		String pwd = configuration.getProperty("db.password");
		int maxConnections = configuration.getPropertyAsInteger("db.maxConnections", 200);
			
		db = configuration.getProperty("db.database","step");

		ServerAddress address = new ServerAddress(host, port);
		List<MongoCredential> credentials = new ArrayList<>();
		if(user!=null) {
			MongoCredential credential = MongoCredential.createCredential(user, db, pwd.toCharArray());
			credentials.add(credential);
		}
		MongoClientOptions.Builder clientOptions = new MongoClientOptions.Builder();
		MongoClientOptions options = clientOptions.connectionsPerHost(maxConnections).build();
		mongoClient = new MongoClient(address, credentials,options);
		
	}
	
	public MongoDatabase getMongoDatabase() {
		return mongoClient.getDatabase(db);
	}
	
	public org.jongo.MongoCollection getJongoCollection(String collectionName) {
		@SuppressWarnings("deprecation")
		DB db = mongoClient.getDB(this.db);
		
		JacksonMapper.Builder builder = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder.registerModule(m));
		
		Jongo jongo = new Jongo(db,builder.build());
		MongoCollection collection = jongo.getCollection(collectionName);
		
		return collection;
	}

	@Override
	public void close() throws IOException {
		mongoClient.close();
	}
	
}
