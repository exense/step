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
package step.plugins.postgres;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class PostgresControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(PostgresControllerPlugin.class);

	//is it the rights place to create the connection//do we need some pooling impl?
	public static String ConnectionPoolName = "jdbc:apache:commons:dbcp:postgresStepDB";

	private GenericObjectPool connectionPool;


	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(PostgresPluginServices.class);

		ch.exense.commons.app.Configuration stepProperties = context.getConfiguration();
		//use to get JDBC url and other required properties
		String jdbcUrl = stepProperties.getProperty("plugins.postgres.jdbc.url","jdbc:postgresql://localhost/step");
		String user = stepProperties.getProperty("plugins.postgres.jdbc.user","postgres");
		String pwd = stepProperties.getProperty("plugins.postgres.jdbc.password","init");
		/*Properties props = new Properties();
		props.setProperty("user","postgres");
		props.setProperty("password","init");
		props.setProperty("ssl","false");*/

		//ref http://commons.apache.org/proper/commons-dbcp/apidocs/org/apache/commons/dbcp2/package-summary.html#package_description
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcUrl,user,pwd);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
		poolableConnectionFactory.setPoolStatements(true);
		poolableConnectionFactory.setDefaultReadOnly(Boolean.FALSE);
		poolableConnectionFactory.setDefaultAutoCommit(Boolean.TRUE);
		connectionPool = new GenericObjectPool(poolableConnectionFactory);
		//need to reference back the factory
		poolableConnectionFactory.setPool(connectionPool);
		PoolingDriver driver = new PoolingDriver();
		//register pool, connection can then be retrieved with DriverManager.getConnection(PostgresControllerPlugin.ConnectionPoolName)
		driver.registerPool("postgresStepDB", connectionPool);

		//TODO Would create the database and table here and any indexes here if required

	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		if (connectionPool!=null) {
			connectionPool.close();
		}
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new PostgresPlugin();
	}

}
