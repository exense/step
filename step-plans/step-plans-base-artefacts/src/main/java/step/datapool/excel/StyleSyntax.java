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

import java.awt.Color;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StyleSyntax {

    /**
     * Analysiert den angegebenen Stil und setzt diesen excel-konform.
     * 
     * @param strStyle strStyle
     * @param _wb _wb
     * @return XSSFCellStyle
     */
    public static XSSFCellStyle composeStyle(String strStyle, Workbook _wb) {
    	if(_wb instanceof XSSFWorkbook) {
    		XSSFWorkbook wb = (XSSFWorkbook) _wb;
    		if (strStyle == null || strStyle.isEmpty()) return null;
    		XSSFCellStyle style = wb.createCellStyle();
    		XSSFFont font = wb.createFont();
    		strStyle = replaceColors(strStyle);
    		
    		/* Unterteilung der verschiedenen Stilangaben: Schriftschnitt, Vordergrund/Hintergrundfarbe, Schriftgroesse, Schriftsatz */
    		String [] arr = strStyle.split(",");
    		for (String str : arr){
    			/* Abhandlung des Schriftschnitts. Diese koennen alle zusammen auftreten und schliessen sich nicht aus */
    			if (str.matches("( *bold *| *italic *| *underline *| *strikeout *)*")){
    				if (str.contains("bold")){
						font.setBold(true);
    				}
    				if (str.contains("italic")){
    					font.setItalic(true);
    				}
    				if (str.contains("underline")){
    					font.setUnderline(Font.U_SINGLE);
    				}
    				if (str.contains("strikeout")){
    					font.setStrikeout(true);
    				}
    				continue;
    			}
    			/* Abhandlung der Farben */
    			if (str.contains("/") && !str.contains(":")){
    				continue;
    			}
    			if (str.contains(":")){
    				String [] fgBg;
    				/* Vordergrund / Hintergrund */
    				if (str.contains("/")){
    					fgBg = str.split("/");	
    				} else {
    					/* Verkuerzte Angabe ohne / (nur Vordergrund) */
    					fgBg = new String [1];
    					fgBg[0] = str;
    				}
    				
    				for (int i = 0; i < fgBg.length; i++){
    					fgBg[i] = fgBg[i].trim();
    					if (!fgBg[i].isEmpty()){
    						String [] clr = fgBg[i].split(":");
    						if (clr.length != 3){
    							return null;
    						}
    						int red = Integer.parseInt(clr[0].trim());
    						int green = Integer.parseInt(clr[1].trim());
    						int blue = Integer.parseInt(clr[2].trim());
    						
    						if (i == 0){
    							/* Schriftfarbe
    							 *  -------------------------------------------------------------
    							 * | ACHTUNG: poi hat einen Fehler beim Setzen der Schriftfarbe! |
    							 *  -------------------------------------------------------------
    							 *  Weiss und Schwarz sind verwechselt worden! Gilt NUR fuer
    							 *  font.setColor!
    							 *  Deshalb wird hier einfach Weiss auf Schwarz korrigiert und
    							 *  umgekehrt.
    							 */
    							if (red == 0 && green == 0 && blue == 0){
    								red = 255; green = 255; blue = 255;
    							} else if (red == 255 && green == 255 && blue == 255){
    								red = 0; green = 0; blue = 0;
    							}
    							XSSFColor xssfColor = new XSSFColor(new Color(red, green, blue), null);
    							font.setColor(xssfColor);
    						}
    						else{
    							// Vordergrund/Hintergrundfarbe der Zelle
    							XSSFColor xssfColor = new XSSFColor(new Color(red, green, blue), null);
    							style.setFillForegroundColor(xssfColor);
    							style.setFillBackgroundColor(xssfColor); 
    							style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    						}
    						
    						
    					}
    				}
    				continue;
    			}
    			/* Abhandlung der Schriftgroesse */
    			if (str.matches(" *[1-9][0-9]* *" )){
    				short fontHeightInPoints = Short.parseShort(str.trim());
    				font.setFontHeightInPoints(fontHeightInPoints);
    				continue;
    			}
    			/* Abhandlung der Schriftart */
    			if (!str.isEmpty()){
    				font.setFontName(str);
    				continue;
    			}
    		}
    		style.setFont(font);
    		return style;
    	} else {
    		return null;
    	}
	}

	/**
	 * Ersetzt vordefinierte Farben mit RGB-Werten.
	 * @param strStyle   "red/green"
	 * @return           "255:0:0/0:255:0"
	 */
	private static String replaceColors(String strStyle) {
		strStyle = strStyle.replaceAll("black", "0:0:0");
		strStyle = strStyle.replaceAll("white", "255:255:255");
		strStyle = strStyle.replace("red", "255:0:0");
		strStyle = strStyle.replace("green", "0:255:0");
		strStyle = strStyle.replace("blue", "0:0:255");
		strStyle = strStyle.replace("yellow", "255:255:0");
		strStyle = strStyle.replace("violet", "255:0:255");
		strStyle = strStyle.replace("cyan", "0:255:255");
		strStyle = strStyle.replace("grey", "128:128:128");
		strStyle = strStyle.replace("gray", "128:128:128");
		return strStyle;
	}

}
