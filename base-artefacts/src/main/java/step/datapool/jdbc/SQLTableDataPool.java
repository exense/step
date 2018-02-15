/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.datapool.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class SQLTableDataPool extends DataSet<SQLTableDataPoolConfiguration> {

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

		this.jdbc_url = configuration.getConnectionString().get();
		this.db_user =  configuration.getUser().get();
		this.db_pwd =  configuration.getPassword().get();
		this.driver_class =  configuration.getDriverClass().get();
		this.writePKey = configuration.getWritePKey().get();
		
		this.query = configuration.getQuery().get();
		this.table = parseQueryForTable(this.query);

		try {
			Class.forName(driver_class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load jdbc driver for class:" + driver_class);
		}
	}

	private static String parseQueryForTable(String query) {
		Pattern p = Pattern.compile("(^|\\s)select.+?from (.+?)(\\s|$)");
		Matcher m = p.matcher(query.toLowerCase());
		if((!m.find()) || (m.groupCount() <3))
			throw new RuntimeException("Could not parse query :" + query);
		else
			return m.group(2);
	}

	public void connect(){

		try {
			conn1 = DriverManager.getConnection(jdbc_url, db_user, db_pwd);
			//conn1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			conn1.setAutoCommit(false);
			} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not connect to the following datapool db :" + jdbc_url + " with user \'" + db_user + "\'");
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
			e.printStackTrace();
			throw new RuntimeException("Could not execute query :" + query);
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
			e.printStackTrace();
			throw new RuntimeException("Could not retrieve result set data from query :" + query);
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
					Object val = rs.getObject(colName);
					row.put(colName,val);
					if(colName.trim().toLowerCase().equals(this.writePKey.trim().toLowerCase()))
						pkValue = val;
				}
				return new SQLRowWrapper(rs.getRow(), row, pkValue);
			}
			else
				return null;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				throw new RuntimeException("Could not retrieve the next row." + rs.getRow());
			} catch (SQLException e1) {
				throw new RuntimeException("Could not retrieve the next row.");
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
		public String put_(String key, String value){
			String sql = null;
			Statement update = null;
			if(pkValue!=null) {
				if(pkValue instanceof String)
					sql = "UPDATE "+table+" SET "+ key +" = \'"+ value + "\' WHERE "+ writePKey + " = '" + pkValue + "'";
				else
					sql= "UPDATE "+table+" SET "+ key +" = \'"+ value + "\' WHERE "+ writePKey + " = " + pkValue;				
			} else {
				throw new RuntimeException("The value of the primary key :" + writePKey + " is null. Unable to update key=" + key + " and value=" + value);
			}
			
			try {
				update = conn1.createStatement();
				update.setQueryTimeout(2);
				/*int upd_rs = */update.executeUpdate(sql);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not execute update with pk :" + writePKey + " = "+pkValue+", with key=" + key + " and value=" + value);
			}finally{
				try {
					if(!update.isClosed())
						update.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			try {
				conn1.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("Commit failed");
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
	}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void close() {
		try {
			conn1.commit();
			if(rs != null && !rs.isClosed())
				rs.close();
			conn1.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void init() {
		connect();
		executeQuery();
	}
	
}
