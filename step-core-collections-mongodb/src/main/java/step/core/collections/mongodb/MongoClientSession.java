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
package step.core.collections.mongodb;

import java.io.Closeable;
import java.io.IOException;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.Configuration;
import step.core.collections.Collection;

public class MongoClientSession implements Closeable {

	protected final MongoClient mongoClient;
	protected final String db;
	
	public MongoClientSession(Configuration configuration) {
		super();

		String host = configuration.getProperty("db.host");
		Integer port = configuration.getPropertyAsInteger("db.port",27017);
		String user = configuration.getProperty("db.username");
		String pwd = configuration.getProperty("db.password");
		// TODO set max connection. how to do this with the new mongo db client is unclear
		//int maxConnections = configuration.getPropertyAsInteger("db.maxConnections", 200);
			
		db = configuration.getProperty("db.database","step");
		
		Builder builder = MongoClientSettings.builder();
		//MongoClientOptions options = clientOptions.connectionsPerHost(maxConnections).build();
		if(user!=null) {
			MongoCredential credential = MongoCredential.createCredential(user, db, pwd.toCharArray());
			builder.credential(credential);
		}
		builder.applyConnectionString(new ConnectionString("mongodb://"+host+":"+port));
		mongoClient = MongoClients.create(builder.build());
		
	}
	
	public MongoDatabase getMongoDatabase() {
		return mongoClient.getDatabase(db);
	}
	
	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public <T> Collection<T> getEntityCollection(String name, Class<T> entityClass) {
		return new MongoDBCollection<T>(this, name, entityClass);
	}

	@Override
	public void close() throws IOException {
		mongoClient.close();
	}
	
}
