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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class SQLTableDataPool extends DataSet<SQLTableDataPoolConfiguration> {

	private Connection conn;
	private Statement smt;
	private ResultSet rs;

	private String jdbc_url;
	private String db_user;
	private String db_pwd;
	private String driver_class;

	private String query;
	private String table;

	private ArrayList<String> cols;

	public SQLTableDataPool(SQLTableDataPoolConfiguration configuration){
		super(configuration);

		String[] split = configuration.getConnectionString().get().trim().split(",");
		this.jdbc_url = split[0];
		this.db_user =  split[1];
		this.db_pwd =  split[2];
		this.driver_class =  split[3];

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
			throw new RuntimeException("Could not parse query for table name :" + query);
		else
			return m.group(2);
	}

	public void connect(){

		try {
			conn = DriverManager.getConnection(jdbc_url, db_user, db_pwd);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not connect to the following datapool db :" + jdbc_url + " with user \'" + db_user + "\'");
		}

	}
	
	public void checkAndreconnect(){
		boolean isValidConn = false;

		if(conn != null){
			try {
				isValidConn = conn.isValid(3);
			}catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("Only trown if timeoutvalue < 0 which obviously can't happen here.");
			}
		}
		if(!isValidConn)
			connect();
	}

	@Override
	public void reset_() {

		checkAndreconnect();

		try {
			smt = conn.createStatement();
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

		checkAndreconnect();

		HashMap<String,Object> row = null;
		
		try {
			if(rs.next()){
				row = new HashMap<String,Object>();
				for (String colName:cols) {
					Object val = rs.getObject(colName);
					row.put(colName,val);
				}
				return new SQLRowWrapper(rs.getRow(), row);
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

	@Override
	public void close() {

		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}


	public class SQLRowWrapper extends SimpleStringMap {

		private final int rowNum;
		private HashMap<String,Object> rowData;

		public SQLRowWrapper(int rowNum, HashMap<String,Object> row) throws Exception {
			super();

			if(rowNum < 1)
				throw new Exception("Invalid row number:" + rowNum);
			this.rowNum = rowNum;
			this.rowData = row; 
		}

		@Override
		public String put(String key, String value){
			String sql = "UPDATE "+table+" SET "+ key +" = \'"+ value + "\' WHERE rownum = " + this.rowNum;
			System.out.println(sql);
			try {
				Statement update = conn.createStatement();
				ResultSet upd_rs = update.executeQuery(sql);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not execute update on row:" + this.rowNum + ", with key=" + key + " and value=" + value);
			}
			
			return value;
		}

		@Override
		public String get(String key) {
			return (String) rowData.get(key);
		}

		public String toString(){return rowData.toString();}
	}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}

}
