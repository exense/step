package step.core.dynamicbeans;

public class TestBean2 {

	DynamicValue<String> testString = new DynamicValue<>("'test'", "js");

	public DynamicValue<String> getTestString() {
		return testString;
	}

	public void setTestString(DynamicValue<String> testString) {
		this.testString = testString;
	}
}
