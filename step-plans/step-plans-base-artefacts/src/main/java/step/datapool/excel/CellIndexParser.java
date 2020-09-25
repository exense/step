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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.util.CellReference;

public class CellIndexParser {

	private static final Pattern CELL_INDEX_PATTERN = Pattern.compile("([A-Z]+)::([0-9]+)");
	
	private static final Pattern CELL_IND_PATTERN_TOLERANT = Pattern.compile("([A-Z]+)[::]?([0-9]+)");
	
    /**
     * Behebung der laestigen Syntax 'A::1'. Folgendes ist auch korrekt:
     * 'A1', 'a::1", 'A1.0', 'a::1.0' 
     * @param cellIndex
     * @return
     */
    private static String normalizeCellIndex(String cellIndex){
    	/* Adressierung normalisieren: trim, uppercase, evt. '.0' abschneiden */
    	String norm = cellIndex.trim();
    	norm = norm.toUpperCase();
    	if (norm.endsWith(".0")){
    		norm = norm.replace(".0", "");
    	}
    	Matcher cellIndexMatcher = CELL_IND_PATTERN_TOLERANT.matcher(norm);
    	if (cellIndexMatcher.matches()){
    		String col = cellIndexMatcher.group(1);
    		String row = cellIndexMatcher.group(2);
    		norm = col + "::" + row;
    	}
    	return norm;
    }
    
    public static CellIndex parse(String cellIndex) {
    	cellIndex = normalizeCellIndex(cellIndex);
    	
    	Integer rowNum;
		Integer column;
		Matcher cellIndexMatcher = CELL_INDEX_PATTERN.matcher(cellIndex);
		if(cellIndexMatcher.matches()) {
			rowNum = Integer.decode(cellIndexMatcher.group(2));
			rowNum--; // In Excel 1-basiert, in poi 0-basiert
			column = CellReference.convertColStringToIndex(cellIndexMatcher.group(1));
			return new CellIndex(column, rowNum);
		} else {
			throw new RuntimeException("Invalid cell index: '" + cellIndex + "'. Valid format are 'A1' or 'A::1'");
		}
    }
    
    public static class CellIndex {
    	int colNum;
    	
    	int rowNum;

		public CellIndex(int colIndex, int rowIndex) {
			super();
			this.colNum = colIndex;
			this.rowNum = rowIndex;
		}

		public int getColNum() {
			return colNum;
		}

		public int getRowNum() {
			return rowNum;
		}
    }
}
