package step.core.accessors;

public class SearchOrder {

	private String attributeName;
	
	private int order;
	
	public SearchOrder(String attributeName, int order) {
		super();
		this.attributeName = attributeName;
		this.order = order;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
