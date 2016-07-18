package step.expressions.placeholder
import groovy.time.TimeCategory;
import java.text.SimpleDateFormat
import step.core.interprete.*

dat = DateFunctions.dat()
last = DateFunctions.last();
first = DateFunctions.first();
today = dat;
now = today;

use (TimeCategory){
	//_TimeCategoryExpression_//
}

/* Falls das Resultat ein Date ist, wird es formatiert. */
if (result instanceof Date) {
	if(input.contains("now")) {
		result = result.format("dd.MM.yyyy kk:mm:ss");
	} else {
		result = result.format("dd.MM.yyyy");
	}
} else {
	result;
}
