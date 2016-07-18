package step.plugins.datatable.formatters;

import java.util.Collection;
import java.util.Iterator;

import com.mongodb.DBObject;

public class ArrayFormatter implements Formatter {

	@Override
	public String format(Object value, DBObject row) {
		if(value!=null && value instanceof Collection) {
			StringBuilder b = new StringBuilder();
			Iterator<?> i = ((Collection<?>)value).iterator();
			while(i.hasNext()) {
				b.append(i.next().toString());
				if(i.hasNext()) {
					b.append(";");
				}
			}
			return b.toString();
		} else {
			return null;
		}
			
	}

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("Not implemented");
	}

}
