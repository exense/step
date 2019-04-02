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
package step.datapool.excel;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCalcMode;

import ch.exense.commons.io.FileHelper;


public class WorkbookFileTest {
	
	private File getResourceFile(String filename) {
		return FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), filename);
	}

	@Test
	public void testRead() {		
		WorkbookFile file = new WorkbookFile(getResourceFile("Excel1.xlsx"), null, false);
		Sheet s = file.getWorkbook().getSheetAt(0);
		Cell c = s.getRow(0).getCell(0);
		String value = c.getStringCellValue();
		file.close();
		Assert.assertEquals("Values", value);	
	}
	
	@Test
	public void testWrite() throws IOException {
		String value = UUID.randomUUID().toString();
		
		File file = getResourceFile("WriteTest2.xlsx");
		if(file.exists()) {
			file.delete();
		}
		
		WorkbookFile workbook = new WorkbookFile(file, null, true);
		
		workbook.getWorkbook().createSheet("test").createRow(0).createCell(0).setCellValue(value);
		workbook.save();
		workbook.close();
		
		workbook = new WorkbookFile(file, null, false);
		Assert.assertEquals(value, workbook.getWorkbook().getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
		
		AssertForceFormulationRecalculationIsEnabled(workbook);
		
		workbook.close();
	}
	
	//TODO write JUnit test for backup excel + forUpdate feature
	
	@Test
	public void testWriteForUpdate() throws IOException {
		String value = UUID.randomUUID().toString();
		
		File file = getResourceFile("WriteTest2.xlsx");
		if(file.exists()) {
			file.delete();
		}

		WorkbookFile workbook = new WorkbookFile(file, null, true, true);

		String sheet = "test";
		
		workbook.getWorkbook().createSheet(sheet).createRow(0).createCell(0).setCellValue(value);
		workbook.save();
		workbook.close();
		
		workbook = new WorkbookFile(file, null, false);
		Assert.assertEquals(value, workbook.getWorkbook().getSheet(sheet).getRow(0).getCell(0).getStringCellValue());
		
		AssertForceFormulationRecalculationIsEnabled(workbook);
		
		workbook.close();
	}
	
//	@Test
	public void testWriteForUpdateFileAlreadyOpened() throws IOException {		
		File file = getResourceFile("WriteTest2.xlsx");
		if(file.exists()) {
			file.delete();
			file.createNewFile();
		}

		Exception ex = null;
		RandomAccessFile access = null;
		FileLock lock = null;
		
		try {
			access = new RandomAccessFile(file,"rw");
			lock = access.getChannel().lock();
			try(WorkbookFile wb = new WorkbookFile(file, null, true, true)) {}			
		} catch(Exception e) {
			ex = e;
			e.printStackTrace();
		} finally {
			try {
				lock.release();
			} catch(IOException e) {};
			access.close();
		}
		
		Assert.assertNotNull(ex);
		Assert.assertTrue(ex.getMessage().contains("is not writable"));
	}


	private void AssertForceFormulationRecalculationIsEnabled(WorkbookFile workbook) {
		CTCalcPr calc = ((XSSFWorkbook)workbook.getWorkbook()).getCTWorkbook().getCalcPr();
		Assert.assertNotNull(calc);
		Assert.assertTrue(calc.getCalcMode()==STCalcMode.AUTO);
	}
	
	@Test
	public void testSizeLimit() {
		Exception ex = null;
		try {
			WorkbookFile workbook = new WorkbookFile(getResourceFile("Excel1.xlsx"), 1, false);
			workbook.close();
		} catch (Exception e) {
			ex = e;
		}
		
		Assert.assertTrue(ex!=null && "The size of the workbook 'Excel1.xlsx' exceeds the max size 1bytes.".equals(ex.getMessage()));
	}
}
