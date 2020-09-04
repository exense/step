package step.repositories.parser

import groovy.time.TimeCategory;
import java.text.DecimalFormat
import java.text.SimpleDateFormat;

abstract class BaseScript extends Script {

    def formatDateStd = "dd.MM.yyyy";
    
    def formatDateTimeStd = "dd.MM.yyyy HH:mm:ss";
    
    def format(Date date, format=formatDateTimeStd) {
    	return date.format(format);
    }
    
    def jetzt(format=formatDateTimeStd) {
		return new Date().format(format);
	}
	
	def heute(format=formatDateStd) {
		return jetzt(format);
	}
	
	def datum(expression) {
		use (TimeCategory){
			return Eval.me(expression);
		}
	}
	
    def ersterTagImMonat(String format=formatDateStd){ 
        Calendar cal = Calendar.getInstance(); 
        cal.setTime(new Date()); 
        cal.set(Calendar.DAY_OF_MONTH, 01); 
        return cal.getTime().format(format); 
    } 

	def ersterTagNaechsterMonat(String format=formatDateStd){ 
        Calendar cal = Calendar.getInstance(); 
        cal.setTime(new Date()); 
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 01);  
        return cal.getTime().format(format); 
    } 
	
	def letzterTagImMonat(String format=formatDateStd){ 
        Calendar cal = Calendar.getInstance(); 
        cal.setTime(new Date()); 
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); 
        return cal.getTime().format(format); 
    }
    
    
}
