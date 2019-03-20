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

public class FormulaPatch {
	
	public static String patch(String formula) {
		/* 
		 * Patch fuer die TEXT-Funktion: die poi-lib kann nicht mit deutschen Formatierungen umgehen. Umgekehrt
		 * meckert Excel in lokalisierter Form, wenn man englische Formatierung einsetzt. Daher werden hier
		 * alle t zu d und j zu y uebersetzt. Zusaetzlich wird der underscor zu minus (da auch dies nicht geht).
		 */
		if (formula.contains("TEXT")){
			/*  
			 * ACHTUNG: es werden nur die Standardformate unterstuetzt:
			 *  CH = "dd.MM.yyyy";
			 *	FR = "dd/MM/yyyy";
			 *	US = "MM/dd/yyyy";
			 *  ISO = "yyyy-MM-dd";
			 *  edv = "yyyyMMdd";
			 */
			formula = formula.replace("tt.MM.jjjj", "dd.mm.yyyy");
			formula = formula.replace("tt/MM/jjjj", "dd/mm/yyyy");
			formula = formula.replace("jjjj-MM-tt", "yyyy-mm-dd");
			formula = formula.replace("jjjjMMtt", "yyyymmdd");
			formula = formula.replace("MM/tt/jjjj", "mm/dd/yyyy");

			formula = formula.replace("tt.mm.jjjj", "dd.mm.yyyy");
			formula = formula.replace("tt/mm/jjjj", "dd/mm/yyyy");
			formula = formula.replace("jjjj-mm-dd", "yyyy-mm-dd");
			formula = formula.replace("jjjjmmtt", "yyyymmdd");
			formula = formula.replace("mm/tt/jjjj", "mm/dd/yyyy");
		}
		
		return formula;
	}

}
