package step.plugins.datatable.formatters;

import com.mongodb.DBObject;

public class RowAsJsonFormatter implements Formatter {
	
	public RowAsJsonFormatter() {
		super();
	}

	@Override
	public String format(Object value, DBObject row) {
		return row.toString();
	}

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("not implemented");
	}

}
