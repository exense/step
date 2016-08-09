package step.datapool.jdbc;

import step.artefacts.ForEachBlock;
import step.datapool.jdbc.SQLTableDataPool.SQLRowWrapper;

public class OracleTableDataSetTest {

	public static void main(String... args){

		String folder =  "jdbc:oracle:thin:@localhost:1521:xe,nairod,nairod,oracle.jdbc.driver.OracleDriver";
		String query = "select rowid, fruit_name, fruit_id from MYFRUITS";  

		ForEachBlock foreach = new ForEachBlock();
		foreach.setFolder(folder);
		foreach.setTable(query);
		SQLTableDataPool ds = new SQLTableDataPool(foreach);
		
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
