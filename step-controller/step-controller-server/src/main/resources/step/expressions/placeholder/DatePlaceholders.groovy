package step.expressions.placeholder
import groovy.time.TimeCategory;
import java.text.SimpleDateFormat
import step.core.interprete.*

	def resolve(String arg) {
		switch(arg) {
		case "today" : return DateFunctions.dat().format("dd.MM.yyyy")
		case "now" : return DateFunctions.dat().format("dd.MM.yyyy kk:mm:ss")
		case "first" : return DateFunctions.first().format("dd.MM.yyyy")
		case "last" : return DateFunctions.last().format("dd.MM.yyyy")
		case "dd" : return DateFunctions.dat().format("dd")
		case "rnd.dd" : return DateFunctions.randomNumber(1,28).toString()
		case "MM" : return DateFunctions.dat().format("MM")
		case "rnd.MM" : return DateFunctions.randomNumber(1,12).toString()
		case "yyyy" : return DateFunctions.dat().format("yyyy")
		case "rnd.yyyy" : return DateFunctions.randomNumber(1900,DateFunctions.dat().format("yyyy").toInteger()).toString()
		case "randomDate" : return DateFunctions.randomDateRange()
		case "rnd.dd.mm.yyyy" : return DateFunctions.randomDateRange()
		case "mm.yyyy" : return DateFunctions.dat().format("MM.yyyy")
		case "dd.mm.yyyy" : return DateFunctions.dat().format("dd.MM.yyyy")
		case "yyyyMMdd" : return DateFunctions.dat().format("yyyyMMdd")
		case "yyyymmdd" : return DateFunctions.dat().format("yyyyMMdd")
		case "hhmmss" : return DateFunctions.dat().format("kkmmss")
		case "hh.mm.ss" : return DateFunctions.dat().format("kk:mm:ss")
		case "hhmm" : return DateFunctions.dat().format("kkmm")
		case "hh.mm" : return DateFunctions.dat().format("kk:mm")
		case "hh" : return DateFunctions.dat().format("kk")
		case "mm" : return DateFunctions.dat().format("mm")
		case "ss" : return DateFunctions.dat().format("ss")
		default: return null
		}
	}
	
	resolve(arg);