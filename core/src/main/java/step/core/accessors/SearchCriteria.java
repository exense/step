package step.core.accessors;


public class SearchCriteria {

	private final String attributeName;
	
	private final String attributeValue;
	
	private final boolean regex;

	public SearchCriteria(String attributeName, String attributeValue) {
		super();
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
		this.regex = false;
	}

	public SearchCriteria(String attributeName, String attributeValue, boolean regex) {
		super();
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
		this.regex = regex;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getAttributeValue() {
		return attributeValue;
	}

	public boolean isRegex() {
		return regex;
	}
}
