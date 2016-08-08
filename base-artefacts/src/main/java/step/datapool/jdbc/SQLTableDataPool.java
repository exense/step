package step.datapool.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import step.artefacts.ForEachBlock;
import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class SQLTableDataPool extends DataSet {

	private Connection conn;
	private Statement smt;
	private ResultSet rs;

	private Object lock;

	private String jdbc_url;
	private String db_user;
	private String db_pwd;
	private String driver_class;

	private String query;

	private ArrayList<String> cols;
	private int cursor;

	public SQLTableDataPool(ForEachBlock configuration) {
		super();

		String[] split = configuration.getFolder().trim().split(",");
		this.jdbc_url = split[0];
		this.db_user =  split[1];
		this.db_pwd =  split[2];
		this.driver_class =  split[3];

		this.query = configuration.getTable();
		this.cursor = 0;

		try {
			Class.forName(driver_class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		this.lock = new Object();
	}



	public void connect(){

		try {
			conn = DriverManager.getConnection(jdbc_url, db_user, db_pwd);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void reset_() {
		synchronized (lock) {
			//TODO: case where conn is working but we want to reset anyway? Should I close and reopen the connection? 

			try {
				if(conn == null || !(conn.isValid(3)));
				connect();
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			}

			try {
				smt = conn.createStatement();
				rs = smt.executeQuery(query);

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
			}
		}
	}

	@Override
	public Object next_() {
		HashMap<String,Object> row = null;
		synchronized (lock) {
			this.cursor++;
			try {
				if(conn == null || !(conn.isValid(3)));
				connect();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if(rs.next()){
					row = new HashMap<String,Object>();
					for (String colName:cols) {
						Object val = rs.getObject(colName);
						row.put(colName,val);
					}
					return new SQLRowWrapper(cursor, row);
				}
				else
					return null;
			} catch (SQLException e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	@Override
	public void close() {
		synchronized (lock) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	private class SQLRowWrapper extends SimpleStringMap {

		private final int rowNum;
		private HashMap<String,Object> rowData;

		public SQLRowWrapper(int rowNum, HashMap<String,Object> row) {
			super();
			this.rowNum = rowNum;
			this.rowData = row; 
		}

		@Override
		public String put(String key, String value) {
			return "SQLTableDataPool.SQLRowWrapper: why would you put something here?";
		}

		@Override
		public String get(String key) {
			return (String) rowData.get(key);
		}

		public String toString(){return rowData.toString();}
	}

}
