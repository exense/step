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

import org.json.JSONObject;

import step.datapool.jdbc.SQLTableDataPool.SQLRowWrapper;

public class OracleTableDataSetTest {

	public static void main(String... args){

		String folder =  "jdbc:oracle:thin:@localhost:1521:xe,nairod,nairod,oracle.jdbc.driver.OracleDriver";
		String query = "select rowid, fruit_name, fruit_id from MYFRUITS";  

		JSONObject b = new JSONObject().put("connectionString", folder)
				.put("query",query);
	
		SQLTableDataPool ds = new SQLTableDataPool(b);
		
		new OracleTableDataSetTest().test1(ds);
		//new OracleTableDataSetTest().test2(ds);
		//new OracleTableDataSetTest().test3(ds);
	}
	
	private void test3(SQLTableDataPool ds){
		ds.reset();
		SQLRowWrapper row = (SQLRowWrapper) ds.next_();
		System.out.println(row);
		row.put("FRUIT_NAME","banana");
		ds.reset();
		row = (SQLRowWrapper) ds.next_();
		System.out.println(row);
	}
	
	
	private void test2(SQLTableDataPool ds){
		ds.reset();
		System.out.println(ds.next());
	}
	
	private void test1(SQLTableDataPool ds){
		ds.reset();
		System.out.println(ds.next());
		System.out.println(ds.next());
		System.out.println(ds.next());
		ds.reset();
		ds.next();
		ds.next();
		ds.close();
		}

}
