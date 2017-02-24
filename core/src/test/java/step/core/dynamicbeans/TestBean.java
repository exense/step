package step.core.dynamicbeans;

public class TestBean {

	DynamicValue<String> testString = new DynamicValue<>("'test'", "js");
	
	DynamicValue<Boolean> testBoolean = new DynamicValue<>("true", "js");
	
	DynamicValue<Integer> testInteger = new DynamicValue<>("10", "js");
	
	DynamicValue<TestBean2> testRecursive = new DynamicValue<>("new step.core.dynamicbeans.TestBean2()", "js");

	public DynamicValue<String> getTestString() {
		return testString;
	}

	public DynamicValue<Boolean> getTestBoolean() {
		return testBoolean;
	}

	public DynamicValue<Integer> getTestInteger() {
		return testInteger;
	}

	public DynamicValue<TestBean2> getTestRecursive() {
		return testRecursive;
	}
	
}
