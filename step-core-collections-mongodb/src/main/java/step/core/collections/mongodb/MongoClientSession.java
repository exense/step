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
import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import ch.exense.commons.app.Configuration;
import com.mongodb.connection.ConnectionPoolSettings;
import step.core.collections.Collection;

public class MongoClientSession implements Closeable {

	protected final MongoClient mongoClient;
	protected final String db;
	protected final Integer batchSize;
	
	public MongoClientSession(Configuration configuration) {
		super();

		String host = configuration.getProperty("db.host");
		Integer port = configuration.getPropertyAsInteger("db.port",27017);
		String user = configuration.getProperty("db.username");
		String pwd = configuration.getProperty("db.password");

		int maxConnections = configuration.getPropertyAsInteger("db.maxConnections", 200);
		Integer minConnections = configuration.getPropertyAsInteger("db.minConnections");
		Integer maxConnectionIdleTimeMs = configuration.getPropertyAsInteger("db.maxConnectionIdleTimeMs");
		Integer maintenanceFrequencyMs = configuration.getPropertyAsInteger("db.maintenanceFrequencyMs");
		Integer maxConnectionLifeTimeMs = configuration.getPropertyAsInteger("db.maxConnectionLifeTimeMs");
		Integer maxWaitTimeMs = configuration.getPropertyAsInteger("db.maxWaitTimeMs");
		batchSize = configuration.getPropertyAsInteger("db.batchSize",1000);

		db = configuration.getProperty("db.database","step");
		
		Builder builder = MongoClientSettings.builder();
		if(user!=null) {
			MongoCredential credential = MongoCredential.createCredential(user, db, pwd.toCharArray());
			builder.credential(credential);
		}
		builder.applyConnectionString(new ConnectionString("mongodb://"+host+":"+port));
		//ref https://mongodb.github.io/mongo-java-driver/4.0/apidocs/mongodb-driver-core/com/mongodb/connection/ConnectionPoolSettings.html
		builder.applyToConnectionPoolSettings(poolSettingBuilder -> {
			poolSettingBuilder.maxSize(maxConnections);
			if (minConnections != null)
				poolSettingBuilder.minSize(minConnections);
			if (maxConnectionIdleTimeMs != null)
				poolSettingBuilder.maxConnectionIdleTime(maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS);
			if (maintenanceFrequencyMs != null)
				poolSettingBuilder.maintenanceFrequency(maintenanceFrequencyMs, TimeUnit.MILLISECONDS);
			if (maxConnectionLifeTimeMs != null)
				poolSettingBuilder.maxConnectionLifeTime(maxConnectionLifeTimeMs, TimeUnit.MILLISECONDS);
			if (maxWaitTimeMs != null)
				poolSettingBuilder.maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS);
			poolSettingBuilder.build();
		});
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

	public Integer getBatchSize() {
		return batchSize;
	}

	@Override
	public void close() throws IOException {
		mongoClient.close();
	}
	
}
