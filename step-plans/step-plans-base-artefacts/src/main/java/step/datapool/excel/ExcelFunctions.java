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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.datapool.excel.CellIndexParser.CellIndex;

public class ExcelFunctions {
	
	private static Logger logger = LoggerFactory.getLogger(ExcelFunctions.class);
	
	private static DecimalFormat customDecimalFormat = null;

	private static Configuration configuration = new Configuration();
	
	/**
	 * Liest einen Wert aus einer Excelzelle. Jeder Zugriff oeffnet die Datei und schliesst
	 * sie danach wieder.
	 * 
	 * @param workbookPath 	Vollstaendiger Pfad zu einer externen Exceldatei oder Dateiname einer
	 * 						als Attachment zugefuegten Exceldatei.
	 * @param sheetName		Name des zu lesenden Blattes
	 * @param cellIndex		Zellenadressierung in der Form 'A::1' (Spalte, Zeile)
	 * @return				Zelleninhalt als String
	 */
	public static String getCell(String workbookPath, String sheetName, String cellIndex) {
		ExcelFileLookup excelFileLookup = new ExcelFileLookup(null);
		File workBookFile = excelFileLookup.lookup(workbookPath);
		return getCell(workBookFile, sheetName, cellIndex);
	}
		
	public static String getCell(File workBookFile, String sheetName, String cellIndex) {		
		try (WorkbookSet workbookSet = new WorkbookSet(workBookFile, getMaxExcelSize(), false, false)) {
			Sheet sheet = getSheet(workbookSet, sheetName, false); 
			
			Cell cell = getCell(sheet, cellIndex, false);
			
			FormulaEvaluator evaluator = workbookSet.getMainFormulaEvaluator();
				
			String result = getCellValueAsString(cell, evaluator);
				
			return result;
		}
	}
	
	protected static Sheet getSheet(WorkbookSet workbookSet, String sheetName, boolean createIfNotExists) {
		Sheet sheet = workbookSet.getMainWorkbook().getSheet(sheetName); 
		
		if(sheet !=null) {
			return sheet;
		} else {
			if(createIfNotExists) {
				return workbookSet.getMainWorkbook().createSheet(sheetName);
			} else {				
				throw new RuntimeException("The sheet '" + sheetName + "' doesn't exist");
			}
		}
	}
	
	private static Cell getCell(Sheet sheet, String cellIndex, boolean createIfNotExists) {
		CellIndex index = CellIndexParser.parse(cellIndex);
		
		Row row = sheet.getRow(index.getRowNum());
		if (row==null) {
			if(createIfNotExists) {
				row = sheet.createRow(index.getRowNum());
			} else {
				throw new RuntimeException("The row of the cell " + cellIndex + " doesn't exist or is empty");
			}
		}
		
		Cell cell = row.getCell(index.getColNum());
		if(cell==null) {
			if(createIfNotExists) {
				cell = row.createCell(index.getColNum());
			} else {				
				throw new RuntimeException("The cell '" + cellIndex + "' doesn't exist or is empty");
			}
		}
				
		return cell;
	}
	

	/**
	 * Schreibt einen Wert in eine Excelzelle. Jeder Zugriff oeffnet die Datei und schliesst
	 * sie danach wieder.
	 * 
	 * @param workbookPath 	Vollstaendiger Pfad zu einer externen Exceldatei. Ob eine angehangte Exceldatei
	 * 						ueberhaupt beschreibbar ist, muss noch eruiert werden.
	 * @param sheetName		Name des zu beschreibenden Blattes
	 * @param cellIndex		Zellenadressierung in der Form 'A::1' (Spalte, Zeile)
	 * @param cellValue		Wert der zu schreiben ist in Form eines Strings
	 * @param style			Font und Stilangaben
	 *    					Ein mit Komma getrennter Eingabestring mit folgenden Teilen:
	 *    
	 *    					<code>schriftschnitt</code>, <code>farbe</code>, <code>schriftgroesse</code>, <code>schriftart</code>
	 *    					<ul>
	 *    					<li> <code>schriftschnitt</code>: bold italic underline strikethrough (Kombinationen moeglich)
	 *    					<li> <code>farbe</code>: red/blue (rote Schrift auf blauem Hintergrund). 0:0:0/255:255:255 (weisse Schrift auf schwarzem Hintergrund)
	 *    					<li> <code>schriftgroesse</code>: 11 (selbe Angabe wie in Excel)
   	 *    					<li> <code>schriftart</code>: Arial (selbe Angabe wie in Excel)
   	 *    					</ul>
	 *    
	 * @throws IOException IOException
	 */
    public static void putCell(String workbookPath, String sheetName, String cellIndex, String cellValue, String style) throws IOException {
    	File workBookFile = new File(workbookPath);
    	try (WorkbookSet workbookSet = new WorkbookSet(workBookFile, getMaxExcelSize(), true, true)) {
			Sheet sheet = getSheet(workbookSet, sheetName, true); 
			
			Cell cell = getCell(sheet, cellIndex, true);
    	
			CellStyle cellStyle = StyleSyntax.composeStyle(style, workbookSet.getMainWorkbook());
			if (cellStyle != null) cell.setCellStyle(cellStyle);
						
			cell.setCellValue(cellValue);
					        
			workbookSet.save();
	    } catch(IOException ex) {	    	
	    	throw ex;
	    }
    }

    /**
	 * Konvertiert unterschiedliche Formate in Strings.
	 * 
	 * @param cell Excel Zelle
	 * @param evaluator FormulaEvaluator
	 * @return Wert der Zelle als String
	 */
	public static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator){
				
		boolean isFormulaPatched = false;
		String initialFormula = null; 
		
		int chkTyp = cell.getCellType();
		if (chkTyp == Cell.CELL_TYPE_FORMULA){
			
			initialFormula = cell.getCellFormula();
			// Some formula have to be changed before they can be evaluated in POI
			String formula = FormulaPatch.patch(initialFormula);
			if(!formula.equals(initialFormula)) {
				isFormulaPatched = true;
				cell.setCellFormula(formula);
				evaluator.notifySetFormula(cell);
			}	
		}
		
		try {
			int typ = evaluateFormulaCell(cell, evaluator);
			if (typ == -1) typ = cell.getCellType();
			switch (typ) {
		        case Cell.CELL_TYPE_NUMERIC:
		        	/* Datum und Zeit (sind auch Zahlen) */
		            if (DateUtil.isCellDateFormatted(cell)) {
		            	Date dat = cell.getDateCellValue();
		            	GregorianCalendar cal = new GregorianCalendar();
		            	cal.setTime(dat);
		            	/*
		            	 * In Excel beginnt die Zeitrechnung am 01.01.1900. Ein Datum ist immer als
		            	 * double gespeichert. Dabei ist der Teil vor dem Dezimalpunkt das Datum
		            	 * und der Teil nach dem Dezimalpunkt die Zeit (z.B. 1.5 entspricht 01.01.1900 12:00:00).
		            	 * Falls der Tag 0 angegeben ist wird der Datumsanteil mit 31.12.1899 zurueck-
		            	 * gegeben. Erhalten wir also ein Jahr kleiner als 1900, dann haben wir eine
		            	 * Zeit.
		            	 */
		            	if (cal.get(Calendar.YEAR) < 1900){ // Zeitformat
		            		SimpleDateFormat STD_TIM = new SimpleDateFormat("kk:mm:ss");
			                return STD_TIM.format(dat);
		            	}
		            
		            	SimpleDateFormat STD_DAT = new SimpleDateFormat("dd.MM.yyyy");
		            	return STD_DAT.format(dat); // Datumsformat
		            } else {
		            	/* int, long, double Formate */
	        			double dbl = cell.getNumericCellValue();
		        		int tryInt = (int)dbl;
		        		long tryLong = (long)dbl;
		        		if (tryInt == dbl){
		        			return new Integer(tryInt).toString(); // int-Format
		        		} else if (tryLong == dbl){
		        			return new Long(tryLong).toString(); // long-Format
		        		}
		        		
		        		// return new Double(dbl).toString(); // double-Format
		        		String numberValueString = new Double(dbl).toString(); // double-Format
		        		
		        		// always use decimal format
		        		try {
	        				// scale 14 to solve problem like value 0.22 --> 0.219999999999997
	        				BigDecimal roundedBigDecimal = new BigDecimal(numberValueString).setScale(14, RoundingMode.HALF_UP); // use constructor BigDecimal(String)!
	        				
	        				String customValueString = getCustomDecimalFormat().format(roundedBigDecimal);
	        				if (!customValueString.equals(numberValueString)) {
	        					logger.debug("getCellValusAsString: Changing string value of double '{}' to '{}'", numberValueString, customValueString);
	        					numberValueString = customValueString; // bigdecimal-format

	        				}
	        			} catch (Exception e) {
	        				logger.error("An error occurred trying to convert the cell value number to decimal format " + numberValueString, e);
	        			}
		        		
		        		return numberValueString;
		        	}
		           
		        case Cell.CELL_TYPE_BOOLEAN:
		            return Boolean.toString(cell.getBooleanCellValue());
		            
		        case Cell.CELL_TYPE_FORMULA:
		        	/* Dieser Fall wird jetzt nie eintreffen, da im Falle einer Formel neu die
		        	 * Berechnung zurueckgegeben wurde, die dann einen eigenen Typ hat.
		        	 */
		            return cell.getCellFormula();
		           
		        case Cell.CELL_TYPE_STRING:
		        	return cell.getRichStringCellValue().getString();
	
		        case Cell.CELL_TYPE_BLANK:
		            return "";
		            
		        case Cell.CELL_TYPE_ERROR:
		        	switch (cell.getErrorCellValue()){
		        		case 1: return "#NULL!";
		        		case 2: return "#DIV/0!";
		        		case 3: return "#VALUE!";
		        		case 4: return "#REF!";
		        		case 5: return "#NAME?";
		        		case 6: return "#NUM!";
		        		case 7: return "#N/A";
		        		default: return "#ERR!";
		        	}
	
		        default:
		        	return "ERROR: unknown Format";
			}
		} finally {
			if(isFormulaPatched) {
				cell.setCellFormula(initialFormula);
				evaluator.notifySetFormula(cell);
			}
		}

	}

	private static int evaluateFormulaCell(Cell cell, FormulaEvaluator evaluator) {
		int typ = -1;
		try {
			typ = evaluator.evaluateFormulaCell(cell);
		} catch (RuntimeException e) {
			String cellRef = CellReference.convertNumToColString(cell.getColumnIndex())+(cell.getRowIndex()+1);
			String errMsg = "Error while evaluating cell " + cellRef + " from sheet " + cell.getSheet().getSheetName() + ": " + e.getMessage();
			throw new RuntimeException(errMsg, e);
		}
		return typ;
	}
	
	private static DecimalFormat getCustomDecimalFormat() {
		if (customDecimalFormat == null) {
			customDecimalFormat = new DecimalFormat("#.##############", DecimalFormatSymbols.getInstance(Locale.ROOT)); // max fractions=14
		}
		
		return customDecimalFormat;
	}

	public static int getMaxExcelSize() {
		return configuration.getPropertyAsInteger("tec.maxexcelsize", 10000000);
	}

	public static void setConfiguration(Configuration configuration) {
		ExcelFunctions.configuration = configuration;
	}
}
