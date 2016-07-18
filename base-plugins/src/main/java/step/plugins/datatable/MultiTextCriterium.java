package step.plugins.datatable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultiTextCriterium implements SearchQueryFactory {

	List<String> attributes;
	
	public MultiTextCriterium(List<String> attributes) {
		super();
		this.attributes = attributes;
	}
	
	public MultiTextCriterium(String... attributes) {
		super();
		this.attributes = Arrays.asList(attributes);
	}

	@Override
	public String createQuery(String attributeName, String expression) {
		StringBuilder query = new StringBuilder();
		query.append("$or:[");
		Iterator<String> it = attributes.iterator();
		while(it.hasNext()) {
			query.append("{"+it.next()+":{$regex:'"+expression+"'}}");
			if(it.hasNext()) {
				query.append(",");
			}
		}
		query.append("]");
		return query.toString();
	}

}
