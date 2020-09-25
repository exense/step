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
package step.datapool.excel;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import junit.framework.Assert;
import ch.exense.commons.io.FileHelper;

import org.junit.Before;
import org.junit.Test;

public class ExcelFunctionsTest {
	
	@Before
	public void setUp() {
		Locale.setDefault(new Locale("de"));
	}

	@Test
	public void testRead() {
		Assert.assertEquals("Values",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "Foglio1", "A::1"));
	}
	
	@Test
	public void testCellTypes() {
		Assert.assertEquals("01.01.2016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B1"));
		Assert.assertEquals("12:00:00",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B2"));
		Assert.assertEquals("100",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B3"));
		Assert.assertEquals("100.1",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B4"));
		Assert.assertEquals(Boolean.toString(true),ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B5"));
		Assert.assertEquals("String with\nnew line",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B6"));
		Assert.assertEquals("0.22",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B7")); // 0.219999999999997
		Assert.assertEquals("0.016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B8")); // 0.016000000000001
		Assert.assertEquals("0.01677777777777",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B9")); // 0.016777777777771
		Assert.assertEquals("0.01677777777778",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "CellTypes", "B10")); // 0.016777777777779
	}
	
	@Test
	public void testErrors() {
		assertException(()->ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "Errors", "B1"), "Error while evaluating cell B1 from sheet Errors:");
		assertException(()->ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "Errors", "B2"), "Error while evaluating cell B2 from sheet Errors:");
	}
	
	@Test
	public void testDates() {
		Assert.assertEquals("01.01.2000",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "Dates", "A1"));
		Assert.assertEquals("12:02:00",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "Dates", "A2"));
	}
	
	@Test
	public void testTEXT() {
		Assert.assertEquals("01.01.2016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B1"));
		Assert.assertEquals("01/01/2016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B2"));
		Assert.assertEquals("2016-01-01",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B3"));
		Assert.assertEquals("20160101",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B4"));
		Assert.assertEquals("01/01/2016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B5"));

		Assert.assertEquals("8 Februar 2016",ExcelFunctions.getCell(getResourceFile("Excel1.xlsx"), "TEXT", "B6"));

	}
	
	@Test
	public void testWriteExcel() throws IOException {
		String value = UUID.randomUUID().toString();

		ExcelFunctions.putCell(getResourceFile("WriteTest.xlsx").getAbsolutePath(), "Foglio1", "A::1", value, null);
		String actual = ExcelFunctions.getCell(getResourceFile("WriteTest.xlsx"), "Foglio1", "A::1");
		
		Assert.assertEquals(value, actual);
	}
	
	@Test
	public void testCreateExcel() throws IOException {
		File file = new File("CreateExcelTest.xlsx");
		if(file.exists()) {
			file.delete();
		}
		file.deleteOnExit();
		
		String value = UUID.randomUUID().toString();

		ExcelFunctions.putCell(file.getAbsolutePath(), "Foglio1", "A1", value, null);
		String actual = ExcelFunctions.getCell(file.getAbsolutePath(), "Foglio1", "A1");
		
		Assert.assertEquals(value, actual);
	}
	
	public static File getResourceFile(String filename) {
		return FileHelper.getClassLoaderResourceAsFile(ExcelFunctionsTest.class.getClassLoader(), filename);
	}
	
	public void assertException(Runnable r, String msg) {
		Exception expected = null;
		try {
			r.run();
		} catch (Exception e) {
			expected = e;
		}
		Assert.assertNotNull(expected);
		Assert.assertTrue(expected.getMessage().contains(msg));
	}
	
}
