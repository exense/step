package step.plugins.datatable;

public class TextCriterium implements SearchQueryFactory {

	@Override
	public String createQuery(String attributeName, String expression) {
		StringBuilder query = new StringBuilder();
		query.append(attributeName);
		query.append(":");
		query.append("{$regex:'");
		query.append(expression);
		query.append("'}");

		return query.toString();
	}

}
