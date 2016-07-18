package step.core.accessors;

import java.net.UnknownHostException;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

import step.commons.conf.Configuration;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class MongoDBAccessorHelper {

	private static MongoDBAccessorHelper INSTANCE = new MongoDBAccessorHelper();

	private String host;
	
	public MongoDBAccessorHelper() {
		super();
		
		host = Configuration.getInstance().getProperty("db.host");
	}

	public static MongoDBAccessorHelper getInstance() {
		return INSTANCE;
	}
	
	public MongoCollection getCollection(String collectionName) {
		MongoClient mongoClient;
		try {
			mongoClient = new MongoClient(host);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		DB db = mongoClient.getDB( "step" );
		
		Jongo jongo = new Jongo(db);
		MongoCollection reports = jongo.getCollection(collectionName);
		
		return reports;
	}
	
	public static MongoClient getClient() {
		try {
			String host = Configuration.getInstance().getProperty("db.host");
			return new MongoClient(host);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static MongoCollection getCollection(MongoClient client, String collectionName) {
		DB db = client.getDB( "step" );
		
		Jongo jongo = new Jongo(db);
		MongoCollection collection = jongo.getCollection(collectionName);
		
		return collection;
	}
	
}
