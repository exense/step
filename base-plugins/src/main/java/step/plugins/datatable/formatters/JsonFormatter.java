package step.plugins.datatable.formatters;

import com.mongodb.DBObject;

public class JsonFormatter implements Formatter {
	
	public JsonFormatter() {
		super();
	}

	@Override
	public String format(Object value, DBObject row) {
		return value.toString();
	}

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("not implemented");
	}

}
