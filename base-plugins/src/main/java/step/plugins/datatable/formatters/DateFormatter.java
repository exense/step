package step.plugins.datatable.formatters;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.mongodb.DBObject;

public class DateFormatter implements Formatter {

	SimpleDateFormat format;
	
	public DateFormatter(String format) {
		this.format = new SimpleDateFormat(format);
	}
	
	@Override
	public String format(Object value, DBObject row) {
		synchronized (format) {
			return format.format(new Date((long) value));
		}
	}

	@Override
	public Object parse(String formattedValue) {
		// TODO Auto-generated method stub
		return null;
	}

}
