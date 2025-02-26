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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class SQLTableDataPool extends DataSet<SQLTableDataPoolConfiguration> {

	protected static Logger logger = LoggerFactory.getLogger(SQLTableDataPool.class);

	protected static final Pattern TABLENAME_PATTERN = Pattern.compile("(^|\\s)select.+?from (\\S+)(\\s|$)", Pattern.CASE_INSENSITIVE);


	private Connection conn1;
	private Statement smt;
	private ResultSet rs = null;

	private String jdbc_url;
	private String db_user;
	private String db_pwd;
	private String driver_class;

	private String query;
	private String table;
	private String writePKey;

	private ArrayList<String> cols;

	public SQLTableDataPool(SQLTableDataPoolConfiguration configuration){
		super(configuration);

		this.driver_class =  configuration.getDriverClass().get();
		try {
			Class.forName(driver_class);
		} catch (ClassNotFoundException e) {
			logger.error("Could not load jdbc driver for class:" + driver_class, e);
			throw new RuntimeException("Could not load jdbc driver for class:" + driver_class +", Underlying exception message:" + e.getMessage());
		}

		this.jdbc_url = configuration.getConnectionString().get();
		this.db_user =  configuration.getUser().get();
		this.db_pwd = configuration.getPassword().get();
		this.writePKey = configuration.getWritePKey().get();

		this.query = configuration.getQuery().get();

		// If we want to be able to write to the DB, we need a bit more preparation
		if (configuration.getForWrite().get()) {
			if (this.writePKey != null) {
				String table = findTableNameInQuery(this.query);
				if (table != null) {
					this.table = table;
				} else {
					logger.warn("Could not determine table name from SQL query, DataSet will not be updatable:" + query);
				}
			} else {
				logger.warn("DataSet is configured for writing, but does not specify a primary key. DataSet will not be updatable.");
			}
		}
	}

	private static String findTableNameInQuery(String query) {
		/* SQL queries can be arbitrarily complex, and this pattern will simply find the next "word" following
		 a "SELECT... FROM ". This is not guaranteed to always be (semantically) correct, as it can be misled in multiple ways,
		 e.g. it would also interpret "table1,table2" as a single table name, or it could interpret only a part
		 of the query (e.g. FROM table1 JOIN table2 ON...). But it's impossible to really tell without
		 having a full-blown SQL parser. One could also think about validating the found name by looking it up in the
		 DB Metadata, but (a) this can only be done after a connection is established, and (b) there are still many
		 possible variants (with/without schema, with/without quotes,....) which all differ slightly between DBMSs,
		 and correctly supporting all the special cases would be a major task by itself.
		 All of this is only relevant if the user marks the DataSet as "for update" anyway, so we assume that they
		 are using "compatible" queries in that case.
		*/
		Matcher m = TABLENAME_PATTERN.matcher(query);
		if((!m.find()) || (m.groupCount() <3)) {
			return null;
		}
		else {
			return m.group(2);
		}
	}

	public void connect(){
		String password = context.getResolver().resolve(db_pwd);
		
		try {
			conn1 = DriverManager.getConnection(jdbc_url, db_user, password);
			//conn1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			conn1.setAutoCommit(false);
		} catch (SQLException e) {
			logger.error("Could not connect to the following datapool db :" + jdbc_url + " with user \'" + db_user + "\'", e);
			throw new RuntimeException("Could not connect to the following datapool db :" + jdbc_url + " with user \'" + db_user + "\', Underlying exception message:" + e.getMessage());
		}
	}

	@Override
	public void reset() {
		executeQuery();
	}

	public void executeQuery(){
		try {
			smt = conn1.createStatement();
			if(rs != null && !rs.isClosed())
				rs.close();
			rs = smt.executeQuery(query);
		} catch (SQLException e) {
			logger.error("Could not execute query :" + query, e);
			throw new RuntimeException("Could not execute query :" + query+ ", Underlying exception message:" + e.getMessage());
		}
		try {
			//get metadata
			ResultSetMetaData meta = null;
			meta = rs.getMetaData();

			//get column names
			int colCount = meta.getColumnCount();
			cols = new ArrayList<String>();
			for (int index=1; index<=colCount; index++)
				cols.add(meta.getColumnName(index));

		} catch (SQLException e) {
			logger.error("Could not retrieve result set data from query :" + query, e);
			throw new RuntimeException("Could not retrieve result set data from query :" + query+ ", Underlying exception message:" + e.getMessage());
		}
	}

	@Override
	public Object next_(){

		ConcurrentHashMap<String,Object> row = null;

		try {
			if(rs.next()){
				row = new ConcurrentHashMap<String,Object>();
				Object pkValue = null;
				for (String colName:cols) {
					if(colName != null) {
						Object val = rs.getObject(colName);
						// Turn null values to empty strings for convenience
						if(val == null)
							val = "";
						row.put(colName,val);
						if(colName.trim().toLowerCase().equals(this.writePKey.trim().toLowerCase()))
							pkValue = val;
					}else {
						logger.error("Null column name.");
						throw new RuntimeException("Null column name.");
					}
				}
				return new SQLRowWrapper(rs.getRow(), row, pkValue);
			}
			else
				return null;
		} catch (Exception e) {
			logger.error("An exception occured while iterating on the dataset.", e);
			try {
				throw new RuntimeException("Could not retrieve the next row: rowId=" + rs.getRow() + ", Underlying exception message: " + e.getMessage());
			} catch (SQLException e1) {
				throw new RuntimeException("Could not retrieve the next row."+ ", Underlying exception message 1 : " + e.getMessage()+ ", Underlying exception message 2: " + e1.getMessage());
			}
		}
	}

	public class SQLRowWrapper extends SimpleStringMap {

		private final Object pkValue;

		private ConcurrentHashMap<String,Object> rowData;

		public SQLRowWrapper(int rowNum, ConcurrentHashMap<String,Object> row, Object pkValue) throws Exception {
			super();
			this.pkValue = pkValue;
			if(rowNum < 1)
				throw new Exception("Invalid row number:" + rowNum);
			this.rowData = row; 
		}

		@Override
		//public synchronized String put(String key, String value){
		public String put(String key, String value){
			String sql = null;
			Statement update = null;
			if (table == null) {
				String msg = "Unable to update DataSet because table name could not be determined";
				logger.error(msg);
				throw new RuntimeException(msg);
			}
			if(pkValue!=null) {
				if(pkValue instanceof String)
					sql = "UPDATE "+table+" SET "+ key +" = \'"+ value + "\' WHERE "+ writePKey + " = '" + pkValue + "'";
				else
					sql= "UPDATE "+table+" SET "+ key +" = \'"+ value + "\' WHERE "+ writePKey + " = " + pkValue;				
			} else {
				logger.error("The value of the primary key :" + writePKey + " is null. Unable to update key=" + key + " and value=" + value);
				throw new RuntimeException("The value of the primary key :" + writePKey + " is null. Unable to update key=" + key + " and value=" + value);
			}

			try {
				update = conn1.createStatement();
				update.setQueryTimeout(2);
				/*int upd_rs = */update.executeUpdate(sql);
			} catch (SQLException e) {
				logger.error("Could not execute update with pk :" + writePKey + " = "+pkValue+", with key=" + key + " and value=" + value, e);
				throw new RuntimeException("Could not execute update with pk :" + writePKey + " = "+pkValue+", with key=" + key + " and value=" + value + ", Underlying exception message: " + e.getMessage());
			}finally{
				try {
					if(!update.isClosed())
						update.close();
				} catch (SQLException e) {
					logger.error("Could not close update connection", e);
					throw new RuntimeException("Could not close update connection" + ", Underlying exception message: " + e.getMessage());
				}
			}
			try {
				conn1.commit();
			} catch (SQLException e) {
				logger.error("Could not commit. ", e);
				throw new RuntimeException("Commit failed"+ ", Underlying exception message: " + e.getMessage());
			}
			rowData.put(key,value);
			return value;
		}

		@Override
		public String get(String key) {
			return (String) rowData.get(key);
		}

		public String toString(){return rowData.toString();}

		@Override
		public int size() {
			return rowData.size();
		}

		@Override
		public boolean isEmpty() {
			return rowData.isEmpty();
		}

		@Override
		public Set<String> keySet() {
			return rowData.keySet();
		}
	}

	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void close() {
		super.close();
		try {
			conn1.commit();
			if(rs != null && !rs.isClosed())
				rs.close();
			conn1.close();
		} catch (SQLException e) {
			logger.error("Could not close close dataset properly", e);
			throw new RuntimeException("Could not close close dataset properly" + ", Underlying exception message: " + e.getMessage());
		}

	}

	@Override
	public void init() {
		super.init();
		connect();
		executeQuery();
	}

}
