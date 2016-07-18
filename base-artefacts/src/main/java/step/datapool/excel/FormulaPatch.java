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
