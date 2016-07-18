package step.plugins.datatable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateRangeCriterium implements SearchQueryFactory {
	
	private SimpleDateFormat DATE_FORMAT;
	
	public DateRangeCriterium(String dateFormat) {
		super();
		
		DATE_FORMAT = new SimpleDateFormat(dateFormat);
	}

	@Override
	public String createQuery(String attributeName, String expression) {

		try {
			Date from;
			synchronized (DATE_FORMAT) {
				from = DATE_FORMAT.parse(expression);				
			}
			
			Calendar c = Calendar.getInstance();
			c.setTime(from);
			c.add(Calendar.DATE, 1);
			
			Date to = c.getTime();
			
			return attributeName+": {$lt: "+to.getTime()+",$gte: "+from.getTime()+"}";
		} catch (ParseException e) {
			return null;
		}
	}

}
