package step.expressions.placeholder

import groovy.time.TimeCategory;
import java.text.SimpleDateFormat;


class DateFunctions {
	
	def static randomDateRange(from = "01.01.1900", to = dat().format("dd.MM.yyyy"), informat = "dd.MM.yyyy", outformat = "dd.MM.yyyy"){
        GregorianCalendar cal = new GregorianCalendar();
		// Datumsformat gemaess inFormat
		SimpleDateFormat sdf = new SimpleDateFormat(informat);
		// Datumsstring parsen
		Date fromDat = sdf.parse(from);
		Date toDat = sdf.parse(to);

		cal.setTime(fromDat);
		
		//1000 ms = 1s, 60s = 1 min, 60 min = 1h, 24h = 1 day
        int days =  (int) ((toDat.getTime() - fromDat.getTime())/(1000*60*60*24) + 1);
		int addDays = randomNumber(0, days);
		cal.add(Calendar.DAY_OF_MONTH, addDays);
		Date dat = cal.getTime();
		sdf = new SimpleDateFormat(outformat);
		return sdf.format(dat);
    }

    def static randomNumber(from = 1, to = 100){
		if (to < from){
			int buf = to;
			to = from; from = buf;
		}
		int interval = to - from + 1;
		double rnd = Math.random() * interval + from;
		return (int)Math.floor(rnd);
    }
    
	def static randomDay(from = 1, to = 28){
		int rndDay = randomNumber(from, to);
		if (rndDay < 10) return "0" + rndDay.toString();
		else return rndDay.toString();
	}
	
	def static randomMonth(from = 1, to = 12){
		int rndMonth = randomNumber(from, to);
		if (rndMonth < 10) return "0" + rndMonth.toString();
		else return rndMonth.toString();
	}

    def static dat() {
    	return new Date();
    }

    def static first() {
         Date d = new Date();
         Calendar cal = d.toCalendar()
         cal.set(Calendar.DAY_OF_MONTH, 1);
         d = cal.getTime();
         return d;
    }

    def static last() {
        Date d = new Date();
        Calendar cal = d.toCalendar()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        d = cal.getTime();
        return d;
    }
    
    	/* -------- Konvertierungsfunktionen ----------------- */
    def static toDate(myDate, myFormat = "dd.MM.yyyy"){
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
}


