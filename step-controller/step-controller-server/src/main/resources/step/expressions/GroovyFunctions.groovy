package step.expressions

import groovy.time.TimeCategory;
import java.text.DecimalFormat
import java.text.SimpleDateFormat;

abstract class GroovyFunctions extends Script {

    def std = "dd.MM.yyyy";
    def frz = "dd/MM/yyyy";
    def iso = "yyyy-MM-dd";
    def edv = "yyyyMMdd";
    def ami = "MM/dd/yyyy";
	def lng = "dd.MM.yyyy kk:mm:ss";
	def tim = "kk:mm:ss";
	
    def dat = new Date();
    def today = dat;
    def now = today;
    def first = first();
    def last = last();
    def dd = dat.format("dd");
    def rnd_dd = randomDay();
    def MM = dat.format("MM");
    def rnd_MM = randomMonth();
    def yyyy = dat.format("yyyy");
    def rnd_yyyy = randomNumber(1900, yyyy.toInteger());
    def randomDate = randomDateRange();
    def rnd_dd_mm_yyyy = randomDate;
    def mm_yyyy = dat.format("MM.yyyy");
    def dd_mm_yyyy = dat.format(std);
    def yyyyMMdd = dat.format(edv);
    def yyyymmdd = yyyyMMdd;
    def last_MM_yyyy = last;
    def hhmmss = dat.format("kkmmss");
    def hh_mm_ss = dat.format("kk:mm:ss");
    def hhmm = dat.format("kk:mm");
    def hh_mm = dat.format("kk:mm");
    def hh = dat.format("kk");
    def mm = dat.format("mm");
    def ss = dat.format("ss");
	
	def propertyMissing(String name) { "" }
	
	
	
    def randomDateRange(from = "01.01.1900", to = dat.format(std), informat = std, outformat = std){
        GregorianCalendar cal = new GregorianCalendar();
		// Datumsformat gemaess inFormat
		SimpleDateFormat sdf = new SimpleDateFormat(informat);
		Date toDat, fromDat;
		if (from instanceof Date){
			fromDat = from;
		} else {
			// Datumsstring parsen
			fromDat = sdf.parse(from);
		}
		if (to instanceof Date){
			toDat = to; 
		} else {
			toDat = sdf.parse(to);
		}
		cal.setTime(fromDat);
		
		//1000 ms = 1s, 60s = 1 min, 60 min = 1h, 24h = 1 day
        int days =  (int) ((toDat.getTime() - fromDat.getTime())/(1000*60*60*24) + 1);
		int addDays = randomNumber(0, days);
		cal.add(Calendar.DAY_OF_MONTH, addDays);
		Date dat = cal.getTime();
		sdf = new SimpleDateFormat(outformat);
		return sdf.format(dat);
    }
    
    def Rnd(from = 1, to = 100){
    	return randomNumber(from, to);
    }
    
	def RandomDate(from = "01.01.1900", to = dat.format(std), informat = std, outformat = std){
		return randomDateRange(from, to, informat, outformat);
	}
	def randomDay(from = 1, to = 28){
		int rndDay = randomNumber(from, to);
		if (rndDay < 10) return "0" + rndDay.toString();
		else return rndDay.toString();
	}
	
	def randomMonth(from = 1, to = 12){
		int rndMonth = randomNumber(from, to);
		if (rndMonth < 10) return "0" + rndMonth.toString();
		else return rndMonth.toString();
	}

    def randomNumber(from = 1, to = 100){
		/* 
		 * Falls die erste Zahl kleiner als die zweite ist kann es bei der folgenden
		 * Berechnung zu einem konstanten Wert als Zufallszahl kommen. Naemlich immer dann
		 * Wenn to - from -1 ergibt und somit das Intervall zu 0 wird. 
		 * Beispiel: randomNumber(1, -1)
		 * Der Benutzer moechte -1, 0, 1 zufaellig. Er bekommt aber immer nur 1 geliefert.
		 * Gilt from <= to kann dies nicht passieren. 
		 * Deshalb werden in solchen Faellen from und to umgekehrt.
		 */
		if (from > to){
			int buf = from;
			from = to;
			to = buf;
		}
		int interval = to - from + 1;
		double rnd = Math.random() * interval + from;
		return (int)Math.floor(rnd);
    }
	def RandomNumber(from = 1, to = 100){
		return randomNumber(from, to);
	}
    def first() {
         Date d = new Date();
         Calendar cal = d.toCalendar()
         cal.set(Calendar.DAY_OF_MONTH, 1);
         d = cal.getTime();
         return d;
    }
    def last() {
        Date d = new Date();
        Calendar cal = d.toCalendar()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        d = cal.getTime();
        return d;
    }
	def StartsWith(String str, String pattern){
		return str.startsWith(pattern);
	}
	def StartsWithTrim(String str, String pattern){
		str = str.trim();
		return str.startsWith(pattern);
	}
	def EndsWith(String str, String pattern){
		return str.endsWith(pattern);
	}
	def EndsWithTrim(String str, String pattern){
		str = str.trim();
		return str.endsWith(pattern);
	}
	def Contains(String str, String pattern){
		return str.contains(pattern);
	}
	def Range(String str, String pattern, int start, int end){
		String sub = str.substring(start, end);
		return sub.contains(pattern); 
	}
	def Replace(String str, String what, String with){
		return str.replace(what, with);
	}

	def IsEmpty(String str){
		if (str == null){
			return true;
		}
		return str.isEmpty();
	}
	
	def IsNotEmpty(String str){
		return !IsEmpty(str);
	}
	
	/**
	 * Liefert den Inhalt eines Files als Text.
	 * @return
	 */
	def ReadFile(String fileNamePath, String charEncoding = "UTF-8"){
		File file = new File(fileNamePath);
		String fileContent = file.getText(charEncoding);
		return fileContent;
	}
	
	/**
	 * F�gt den Text dem File hinzu. Verzeichnis muss existieren.
	 */
	def Boolean WriteFile(String fileNamePath, String fileContent, String charEncoding = "UTF-8") {
		File file = new File(fileNamePath);
		String separator = "";
		if (file.exists() && file.getText().size() > 0) {
			separator = "\n";
		}
		file.append(separator + fileContent, charEncoding);
		return true;
	}
	
	/**
	 * Delete a file if exists.
	 */
	def Boolean DeleteFile(String fileNamePath) {
		File file = new File(fileNamePath);
		if (file.exists()) {
			return file.delete();
		}
		return true;
	}

	/**
	 * Heutiges Datum plus Anzahl Tage, Monate und Jahre (koennen auch negativ sein)
	 * @return
	 */
	def Today(String format = std, int days = 0, int months = 0, int years = 0 ) {
        Date d = new Date();
        Calendar cal = d.toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
        d = cal.getTime();
        return d.format(format);
	}
	/**
	 * Heutiges Datum/Zeit plus Anzahl Sekunden, Minuten, Stunden, Tage, Monate 
	 * und Jahre (koennen auch negativ sein)
	 * @return
	 */
	def Now(String format = lng, int seconds = 0, int minutes = 0, int hours = 0, int days = 0, int months = 0, int years = 0 ) {
		Date d = new Date();
		Calendar cal = d.toCalendar()
		cal.add(Calendar.SECOND, seconds);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		d = cal.getTime();
		return d.format(format);
	}
	/**
	 * Uebergebenes Datum plus Anzahl Tage, Monate
	 * und Jahre (koennen auch negativ sein)
	 * @return
	 */
	def ThisDate(String date = Today(), String inFormat = std, String outFormat = std, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		d = cal.getTime();
		return d.format(outFormat);
	}
	/**
	 * Uebergebenes Datum plus Anzahl Sekunden, Minuten, Stunden, Tage, Monate
	 * und Jahre (koennen auch negativ sein)
	 * @return
	 */
	def ThisTime(String date = Now(), String inFormat = lng, String outFormat = lng, int seconds = 0, int minutes = 0, int hours = 0, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.SECOND, seconds);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		d = cal.getTime();
		return d.format(outFormat);
	}
	/**
	 * Uebergebenes Datum plus Anzahl Tage, Monate
	 * und Jahre (koennen auch negativ sein). Danach der Monatserste.
	 * @return
	 */
    def First(String date = Today(), String inFormat = std, String outFormat = std, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		/* nach allen Additionen wird der Tag auf den 1. und Mitternacht gesetzt */
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
        d = cal.getTime();
        return d.format(outFormat);
    }
	/**
	 * Uebergebenes Datum plus Anzahl Tage, Monate
	 * und Jahre (koennen auch negativ sein). Danach der Monatsletzte.
	 * @return
	 */
    def Last(String date = Today(), String inFormat = std, String outFormat = std, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		/* nach allen Additionen wird der Tag auf den Monatsletzten und kurz vor Tageswechsel gesetzt */
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
        d = cal.getTime();
        return d.format(outFormat);
    }
	/**
	 * Uebergebenes Datum plus Anzahl Tage, Monate
	 * und Jahre (koennen auch negativ sein). Danach der Jahresbeginn.
	 * @return
	 */
    def BegYear(String date = Today(), String inFormat = std, String outFormat = std, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		/* nach allen Additionen wird der Tag/Monat auf den 1.1 und Mitternacht gesetzt */
		cal.set(Calendar.MONTH, 0); // 0=jan
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
        d = cal.getTime();
        return d.format(outFormat);
    }
	/**
	 * Uebergebenes Datum plus Anzahl Tage, Monate
	 * und Jahre (koennen auch negativ sein). Danach das Jahresende kurz vor Mitternacht.
	 * @return
	 */
    def EndYear(String date = Today(), String inFormat = std, String outFormat = std, int days = 0, int months = 0, int years = 0 ) {
		SimpleDateFormat sdf = new SimpleDateFormat(inFormat);
		Date d = sdf.parse(date);
		Calendar cal = d.toCalendar()
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.YEAR, years);
		/* nach allen Additionen wird der 31.12. kurz vor Tageswechsel gesetzt */
		cal.set(Calendar.MONTH, 11); // 11=dec
        cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
        d = cal.getTime();
        return d.format(outFormat);
    }
	/* -------- Konvertierungsfunktionen ----------------- */
    def toDate(myDate, myFormat = std){
		if (myDate instanceof String){
	        SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
	        Date d = sdf.parse(myDate);
	        return d;
		} else if (myDate instanceof Date){
			return myDate;
		} else if (myDate instanceof Calendar){
			return myDate.getTime();
		}
		return myDate;
    }
    def toString(myInput, String format = ""){
        if (myInput instanceof String){
			DecimalFormat decForm = new DecimalFormat(format);
			if (format == "") return myInput.toString();
            dbl = Double.parseDouble(myInput);
            return decForm.format(dbl);
        } else if (myInput instanceof Date){
			if (format == "") return ((Date)myInput).format(std);
			else return ((Date)myInput).format(format);
        } 
		else if (myInput instanceof Double || myInput instanceof Integer || myInput instanceof Long || myInput instanceof Float || myInput instanceof BigDecimal){
			if (format == "") return myInput.toString();
			DecimalFormat decForm = new DecimalFormat(format);
            return decForm.format((Double)myInput);
        }
        return myInput.toString();
    }
	/* Gibt einen int, long oder double zurueck */
	def toNumber(myInput){
		if (myInput instanceof String){
			double dbl = Double.parseDouble(myInput);
			int tryInt = (int)dbl;
			long tryLong = (long)dbl;
			if (tryInt == dbl){ // int passt
				return tryInt;
			} else if (tryLong == dbl){ // long passt
				return tryLong;
			}
			return dbl;
		}
		return myInput;
	}
	def toBoolean(myInput){
		if (myInput instanceof String){
			return Boolean.parseBoolean(myInput);
		}
		return myInput;
	}
	
	def QftOutputVars(String stringMap){
		map = Eval.me(stringMap);
		return map;
	}
	
	def ExcelDateToString(String xlsDate, String format){
		double d = Double.parseDouble(xlsDate);
		Date dat = DateUtil.getJavaDate(d);
		return dat.format(format);
	}
	
}
