package step.plugins.datatable.formatters;

import com.mongodb.DBObject;

public class StringFormatter implements Formatter {

	@Override
	public String format(Object value, DBObject row) {
		return value.toString();
	}

	@Override
	public Object parse(String formattedValue) {
		return formattedValue;
	}

}
