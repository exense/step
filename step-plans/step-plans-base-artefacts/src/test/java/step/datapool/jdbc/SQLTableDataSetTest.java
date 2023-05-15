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
package step.datapool.jdbc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.AbstractArtefactTest;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;
import step.datapool.jdbc.SQLTableDataPool.SQLRowWrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLTableDataSetTest extends AbstractArtefactTest {

	private static String DB_URL = "jdbc:mysql://central-mysql.stepcloud-test.ch:3306/step";
	private static String USER = "step";
	private static String PASS = "myR!4m3RT9";
	private DataSet<?> ds;
	private String table;

	@Before
	public void setup() {
		try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			Statement stmt = conn.createStatement();) {

			table = "Fruits_" + System.currentTimeMillis();
			String sql = "CREATE TABLE " + table +
					" (id INTEGER not NULL, " +
					" fruit_name VARCHAR(255), " +
					" PRIMARY KEY ( id ))";

			stmt.executeUpdate(sql);
			System.out.println("Created table " + table + " in given database...");

			stmt.executeUpdate("INSERT INTO " + table + " VALUES (1, 'banana')");
			stmt.executeUpdate("INSERT INTO " + table + " VALUES (2, 'apple')");
			stmt.executeUpdate("INSERT INTO " + table + " VALUES (3, 'lemon')");

			System.out.println("Inserted records into the table...");

			SQLTableDataPoolConfiguration conf = new SQLTableDataPoolConfiguration();
			conf.setConnectionString(new DynamicValue<>(DB_URL));
			String query = "SELECT * FROM " + table;
			conf.setQuery(new DynamicValue<>(query));
			conf.setDriverClass(new DynamicValue<>("com.mysql.cj.jdbc.Driver"));
			conf.setUser(new DynamicValue<>(USER));
			conf.setPassword(new DynamicValue<>(PASS));
			conf.setForWrite(new DynamicValue<>(false));
			conf.setWritePKey(new DynamicValue<>("id"));
			ds = DataPoolFactory.getDataPool("sql", conf, newExecutionContext());
			ds.init();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() {
		ds.close();
		try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			Statement stmt = conn.createStatement();) {
			String sql = "DROP TABLE " + table;
			stmt.executeUpdate(sql);
			System.out.println("Table deleted in given database...");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testUpdateRow(){
		ds.reset();
		SQLRowWrapper row = (SQLRowWrapper) ds.next_();
		Assert.assertEquals("{fruit_name=banana, id=1}", row.toString());
		row.put("fruit_name","strawberry");
		ds.reset();
		Assert.assertEquals("{fruit_name=strawberry, id=1}", ds.next().toString());
	}


	@Test
	public void testBrowse(){
		ds.reset();
		Assert.assertEquals("{fruit_name=banana, id=1}", ds.next().toString());
		Assert.assertEquals("{fruit_name=apple, id=2}", ds.next().toString());
		Assert.assertEquals("{fruit_name=lemon, id=3}", ds.next().toString());
		Assert.assertNull(ds.next());
		ds.reset();
		Assert.assertEquals("{fruit_name=banana, id=1}", ds.next().toString());
	}

}
