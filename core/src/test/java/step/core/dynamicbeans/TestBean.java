package step.core.dynamicbeans;

public class TestBean {

	DynamicValue<String> testString = new DynamicValue<>("'test'", "js");
	
	DynamicValue<Boolean> testBoolean = new DynamicValue<>("true", "js");
	
	DynamicValue<Integer> testInteger = new DynamicValue<>("10", "js");
	
	DynamicValue<TestBean2> testRecursive = new DynamicValue<>("new step.core.dynamicbeans.TestBean2()", "js");

	TestBean2[] testArray = new TestBean2[]{new TestBean2(), new TestBean2()};
	
	TestBean2 testRecursive2 = new TestBean2();

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

	public void setTestString(DynamicValue<String> testString) {
		this.testString = testString;
	}

	public void setTestBoolean(DynamicValue<Boolean> testBoolean) {
		this.testBoolean = testBoolean;
	}

	public void setTestInteger(DynamicValue<Integer> testInteger) {
		this.testInteger = testInteger;
	}

	public void setTestRecursive(DynamicValue<TestBean2> testRecursive) {
		this.testRecursive = testRecursive;
	}

	@ContainsDynamicValues
	public TestBean2 getTestRecursive2() {
		return testRecursive2;
	}

	public void setTestRecursive2(TestBean2 testRecursive2) {
		this.testRecursive2 = testRecursive2;
	}

	public TestBean2[] getTestArray() {
		return testArray;
	}

	public void setTestArray(TestBean2[] testArray) {
		this.testArray = testArray;
	}
	
}
