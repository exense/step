package step.plugins.datatable.formatters;

import com.mongodb.DBObject;


public interface Formatter {

	public String format(Object value, DBObject row);
	
	public Object parse(String formattedValue);
}
