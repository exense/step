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
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Assert;
import org.junit.Test;

import step.datapool.excel.WorkbookSet.LinkedWorkbookFileResolver;


public class WorkbookSetTest {

	@Test
	public void testExternRef() {
		File file = getResourceFile("Excel1.xlsx");
		
		WorkbookSet set = new WorkbookSet(file, null, new LinkedWorkbookFileResolver() {
			@Override
			public File resolve(String linkedFilename) {
				return getResourceFile(linkedFilename.substring(linkedFilename.lastIndexOf("/")+1));
			}
		}, false, false);
		
		Sheet s = set.getMainWorkbook().getSheetAt(0);
		
		Cell c = s.getRow(1).getCell(0);
		
		CellValue v = set.getMainFormulaEvaluator().evaluate(c);
		
		Assert.assertEquals("Value", v.getStringValue());	
	}

	private File getResourceFile(String filename) {
		if(this.getClass().getClassLoader().getResource(filename)!=null) {
			return new File(this.getClass().getClassLoader().getResource(filename).getFile());
		} else {
			return null;
		}
	}
	
	@Test
	public void testWorkbookSetWrite() throws IOException {
		String value = UUID.randomUUID().toString();
		
		File file = getResourceFile("WriteTest2.xlsx");
		
		WorkbookSet workbookSet = new WorkbookSet(file, null, false, false);
		workbookSet.getMainWorkbook().getSheetAt(0).getRow(0).getCell(0).setCellValue(value);
		workbookSet.save();
		workbookSet.close();
		
		workbookSet = new WorkbookSet(file, null, false, false);
		Assert.assertEquals(value, workbookSet.getMainWorkbook().getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
	}
	
	@Test
	public void testWorkbookSetWriteForUpdate() throws IOException {
		String value = UUID.randomUUID().toString();
		
		File file = getResourceFile("WriteTest2.xlsx");
		
		WorkbookSet workbookSet = new WorkbookSet(file, null, false, true);
		workbookSet.getMainWorkbook().getSheetAt(0).getRow(0).getCell(0).setCellValue(value);
		workbookSet.save();
		workbookSet.close();
		
		workbookSet = new WorkbookSet(file, null, false, false);
		Assert.assertEquals(value, workbookSet.getMainWorkbook().getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
	}
	
	@Test
	public void testSizeLimit() {
		Exception ex = null;
		try {
			new WorkbookSet(getResourceFile("Excel1.xlsx"), 1, false, false);
		} catch (Exception e) {
			ex = e;
		}
		
		Assert.assertTrue(ex!=null && "The size of the workbook 'Excel1.xlsx' exceeds the max size 1bytes.".equals(ex.getMessage()));
	}
}
