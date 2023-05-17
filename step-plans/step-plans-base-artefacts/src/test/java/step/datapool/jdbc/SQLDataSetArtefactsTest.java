package step.datapool.jdbc;

import ch.exense.commons.io.FileHelper;
import org.junit.After;
import org.junit.Before;
import step.artefacts.DataSetArtefact;
import step.artefacts.ForBlock;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.AbstractDataPoolTest;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;
import step.datapool.excel.ExcelDataPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLDataSetArtefactsTest extends AbstractDataPoolTest {

	private static String DB_URL = "jdbc:mysql://central-mysql.stepcloud-test.ch:3306/step";
	private static String USER = "step";
	private static String PASS = "myR!4m3RT9";
	private String table;

	@Before
	public void setup() {
		try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			Statement stmt = conn.createStatement();) {

			table = "SimpleTable_" + System.currentTimeMillis();
			String sql = "CREATE TABLE " + table +
					" (Col1 VARCHAR(255) not NULL, " +
					" Col2 VARCHAR(255), " +
					" PRIMARY KEY ( Col1 ))";

			stmt.executeUpdate(sql);
			System.out.println("Created table " + table + " in given database...");

			stmt.executeUpdate("INSERT INTO " + table + " VALUES ('row11', 'row12')");
			stmt.executeUpdate("INSERT INTO " + table + " VALUES ('row21', 'row22')");

			System.out.println("Inserted records into the table...");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() {
		try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			Statement stmt = conn.createStatement();) {
			String sql = "DROP TABLE " + table;
			stmt.executeUpdate(sql);
			System.out.println("Table deleted in given database...");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	protected boolean supportDataSetUpdate() {
		return true;
	}

	@Override
	protected boolean isInMemory() {
		return false;
	}
	protected DataPoolConfiguration getDataPoolConfiguration() throws IOException {
		SQLTableDataPoolConfiguration conf = new SQLTableDataPoolConfiguration();
		conf.setConnectionString(new DynamicValue<>(DB_URL));
		String query = "SELECT * FROM " + table;
		conf.setQuery(new DynamicValue<>(query));
		conf.setDriverClass(new DynamicValue<>("com.mysql.cj.jdbc.Driver"));
		conf.setUser(new DynamicValue<>(USER));
		conf.setPassword(new DynamicValue<>(PASS));
		conf.setForWrite(new DynamicValue<>(false));
		conf.setWritePKey(new DynamicValue<>("Col1"));

		return conf;
	}

	protected String getDataSourceType() {
		return "sql";
	}

	protected DataSetArtefact getDataSetArtefact(boolean resetAtEnd) throws IOException {
		DataSetArtefact dataSetArtefact = new DataSetArtefact();
		dataSetArtefact.setDataSource(getDataPoolConfiguration());
		dataSetArtefact.setDataSourceType(getDataSourceType());
		dataSetArtefact.setResetAtEnd(new DynamicValue<>(resetAtEnd));
		return dataSetArtefact;
	}

	protected ForBlock getForBlock() throws IOException {
		ForBlock f = new ForBlock();
		f.setDataSourceType(getDataSourceType());
		f.setDataSource(getDataPoolConfiguration());
		f.setItem(new DynamicValue<>("item"));
		return f;
	}



}
