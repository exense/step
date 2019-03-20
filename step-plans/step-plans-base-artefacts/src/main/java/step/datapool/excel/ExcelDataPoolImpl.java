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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ValidationException;
import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class ExcelDataPoolImpl extends DataSet<ExcelDataPool> {
	
	private static Logger logger = LoggerFactory.getLogger(ExcelDataPoolImpl.class);
		
	WorkbookSet workbookSet;
		
	Sheet sheet;
	
	int cursor;
	
	boolean forWrite;
		
	volatile boolean updated = false;
	
	static Pattern crossSheetPattern = Pattern.compile("^(.+?)::(.+?)$");
			
	public ExcelDataPoolImpl(ExcelDataPool configuration) {
		super(configuration);
	}

	@Override
	public void init() {	
		super.init();
		
		String bookName = configuration.getFile().get();
		String sheetName = configuration.getWorksheet().get();
		
		
		logger.debug("book: " + bookName + " sheet: " + sheetName);
		
		ExcelFileLookup excelFileLookup = new ExcelFileLookup(context);
		File workBookFile = excelFileLookup.lookup(bookName);
		
		forWrite = configuration.getForWrite().get();
		workbookSet = new WorkbookSet(workBookFile, ExcelFunctions.getMaxExcelSize(), forWrite, true);

		Workbook workbook = workbookSet.getMainWorkbook();
		
		if (sheetName==null || sheetName.isEmpty()){
			if(workbook.getNumberOfSheets()>0) {
				sheet = workbook.getSheetAt(0);					
			} else {
				if(forWrite) {
					sheet = workbook.createSheet();
				} else {
					throw new ValidationException("The workbook " + workBookFile.getName() + " contains no sheet");						
				}
			}
		} else {
			sheet = workbook.getSheet(sheetName);
			if (sheet == null){
				if(forWrite) {
					sheet = workbook.createSheet(sheetName);
				} else {
					throw new ValidationException("The sheet " + sheetName + " doesn't exist in the workbook " + workBookFile.getName());						
				}
			}
		}
		
		resetCursor();
	}
	
	@Override
	public void reset() {
		resetCursor();
	}

	private void resetCursor() {
		if(configuration.getHeaders().get()) {
			cursor = 0;
		} else {
			cursor = -1;
		}
	}
	
	private int mapHeaderToCellNum(Sheet sheet, String header, boolean createHeaderIfNotExisting) {
		if(configuration.getHeaders().get()) {
			Row row = sheet.getRow(0);
			if(row!=null) {
				for(Cell cell:row) {
					String key = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
					if(key!=null && key.equals(header)) {
						return cell.getColumnIndex();
					}
				}				
			} else {
				if(createHeaderIfNotExisting) {
					sheet.createRow(0);
				} else {
					throw new ValidationException("The sheet " + sheet.getSheetName() + " contains no headers");				
				}
			}
			if(createHeaderIfNotExisting) {
				return addHeader(sheet, header);
			} else {
				throw new ValidationException("The column " + header + " doesn't exist in sheet " + sheet.getSheetName());				
			}
		} else {
			return CellReference.convertColStringToIndex(header);
		}
	}
	
	private int addHeader(Sheet sheet, String header) {
		if(configuration.getHeaders().get()) {
			Row row = sheet.getRow(0);
			Cell cell = row.createCell(Math.max(0, row.getLastCellNum()));
			cell.setCellValue(header);
			updated = true;
			return cell.getColumnIndex();
		} else {
			throw new RuntimeException("Unable to create header for excel configured not to use headers.");							
		}
	}
	
	private List<String> getHeaders() {
		List<String> headers = new ArrayList<>();
		Row row = sheet.getRow(0);
		for(Cell cell:row) {
			String key = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
			headers.add(key);
		}
		return headers;
	}
	
	private static final String SKIP_STRING = "@SKIP"; 

	@Override
	public Object next_() {					
		for(;;) {
			cursor++;
			if(cursor <= sheet.getLastRowNum()){
				Cell cell = (sheet.getRow(cursor)).getCell(0);
				if (cell != null){
					String value = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
					if (value != null && !value.isEmpty()){
						if (value.equals(SKIP_STRING)) {
							continue;
						} else {
							return new RowWrapper(cursor);
						}
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else {
				return null;			
			}
		}
	}

	@Override
	public void save() {
		if(updated) {
			try {
				workbookSet.save();
			} catch (IOException e) {
				throw new RuntimeException("Error writing file " + workbookSet.getMainWorkbookFile().getAbsolutePath(), e);
			}
		}
	}

	@Override
	public void close() {
		super.close();
		
		if(workbookSet!=null) {
			workbookSet.close();				
		}

		sheet = null;
	}
	
	private Cell getCellByID(int cursor, String name) {
		Sheet sheet;
		String colName;
		
		Matcher matcher = crossSheetPattern.matcher(name);
		if(matcher.find()) {
			String sheetName = matcher.group(1);
			colName = matcher.group(2);
			
			sheet = workbookSet.getMainWorkbook().getSheet(sheetName);

			if (sheet == null) {
				throw new ValidationException("The sheet " + sheetName
						+ " doesn't exist in the workbook " + workbookSet.getMainWorkbookFile().getName());
			}
		} else {
			sheet = this.sheet;
			colName = name;
		}
				
		Row row = sheet.getRow(cursor);
		if(row==null) {
			row = sheet.createRow(cursor);
		}
		int cellNum = mapHeaderToCellNum(sheet, colName, false);
		Cell cell = row.getCell(cellNum, Row.CREATE_NULL_AS_BLANK);
		
		return cell;
	}
	
	private class RowWrapper extends SimpleStringMap {
		
		private final int cursor;

		public RowWrapper(int cursor) {
			super();
			this.cursor = cursor;
		}

		@Override
		public Set<String> keySet() {
			Set<String> headers = new HashSet<>(getHeaders());
			return headers;
		}

		@Override
		public String get(String key) {
			synchronized(workbookSet) {
				Cell cell = getCellByID(cursor, key);
				return ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
			}
		}

		@Override
		public String put(String key, String value) {
			synchronized(workbookSet) {
				Cell cell = getCellByID(cursor, key);
				if(cell!=null) {
					updated = true;
						cell.setCellValue(value);
						workbookSet.getMainFormulaEvaluator().notifyUpdateCell(cell);
					}
				return value;
			}	
		}
		
		@Override
		public int size() {
			int tableWidth = getHeaders().size();
			int nonNullCells = 0;
			for(int i = 0; i < tableWidth; i++){
				Cell cell = sheet.getRow(cursor).getCell(i);
				if(cell != null){
					String value = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
					if((value != null) && (!value.isEmpty()))
						nonNullCells++;
				}
			}
			return nonNullCells;
		}

		@Override
		public boolean isEmpty() {
			return (size() < 1) ? true: false;
		}
	}

	@Override
	public void addRow(Object rowInput_) {
		if(rowInput_ instanceof Map) {
			Row row = sheet.createRow(sheet.getLastRowNum()+1);
			Map<?,?> rowInput = (Map<?,?>) rowInput_;
			for(Object keyObject:rowInput.keySet()) {
				if(keyObject instanceof String) {
					int cellNum = mapHeaderToCellNum(sheet, (String)keyObject, true);
					Cell cell = row.createCell(cellNum);
					cell.setCellValue(rowInput.get(keyObject).toString());
					updated = true;
				}
			}
		} else {
			throw new RuntimeException("Add row not implemented for object of type '"+rowInput_.getClass());
		}
	}

	@Override
	public void setContext(ExecutionContext executionContext) {
		this.context = executionContext;
	}
}
