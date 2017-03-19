package step.core.accessors;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import step.commons.conf.Configuration;

public class MongoClientSession implements Closeable {

	MongoClient mongoClient;
	
	String db;
	
	Configuration configuration;

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
		
		db = configuration.getProperty("db.database","step");

		ServerAddress address = new ServerAddress(host, port);
		List<MongoCredential> credentials = new ArrayList<>();
		if(user!=null) {
			MongoCredential credential = MongoCredential.createMongoCRCredential(user, db, pwd.toCharArray());
			credentials.add(credential);
		}
		
		mongoClient = new MongoClient(address, credentials);
	}
	
	@Deprecated
	public DB getDB() {
		return mongoClient.getDB(db);		
	}
	
	public MongoDatabase getMongoDatabase() {
		return mongoClient.getDatabase(db);
	}

	@Override
	public void close() throws IOException {
		mongoClient.close();
	}
	
}
