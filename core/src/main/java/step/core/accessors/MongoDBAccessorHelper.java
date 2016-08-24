package step.core.accessors;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import step.commons.conf.Configuration;

public class MongoDBAccessorHelper {

	private static MongoDBAccessorHelper INSTANCE = new MongoDBAccessorHelper();

	private String host;
	
	private Integer port;
	
	String user;
	
	String pwd;
	
	String db;
	
	public MongoDBAccessorHelper() {
		super();
		
		host = Configuration.getInstance().getProperty("db.host");
		port = Configuration.getInstance().getPropertyAsInteger("db.port",27017);
		user = Configuration.getInstance().getProperty("db.username");
		pwd = Configuration.getInstance().getProperty("db.password");
		db = Configuration.getInstance().getProperty("db.database","step");
	}

	public static MongoDBAccessorHelper getInstance() {
		return INSTANCE;
	}
	
	public static MongoClient getClient() {
		return getInstance().getMongoClient();
	}
	
	public MongoClient getMongoClient() {
		try {
			ServerAddress address = new ServerAddress(host, port);
			List<MongoCredential> credentials = new ArrayList<>();
			if(user!=null) {
				MongoCredential credential = MongoCredential.createMongoCRCredential(user, db, pwd.toCharArray());
				credentials.add(credential);
			}
			return new MongoClient(address, credentials);
		} catch (UnknownHostException e1) {
			throw new RuntimeException(e1);
		}
		
		
	}
	
	public static MongoCollection getCollection(MongoClient client, String collectionName) {
		return getInstance().getMongoCollection(client, collectionName);
	}
	
	public MongoCollection getMongoCollection(MongoClient client, String collectionName) {
		DB db = client.getDB(this.db);
		
		Jongo jongo = new Jongo(db);
		MongoCollection collection = jongo.getCollection(collectionName);
		
		return collection;
	}
	
}
