/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import step.core.collections.Collection;
import step.core.collections.mongodb.MongoDBCollection;

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
	
	public <T> Collection<T> getEntityCollection(String name, Class<T> entityClass) {
		return new MongoDBCollection<T>(this, name, entityClass);
	}

	@Override
	public void close() throws IOException {
		mongoClient.close();
	}
	
}
