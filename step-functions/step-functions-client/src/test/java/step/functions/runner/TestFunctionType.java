package step.functions.runner;

import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.grid.filemanager.FileVersionId;

public class TestFunctionType extends AbstractFunctionType<TestFunction> {

	@Override
	public String getHandlerChain(TestFunction function) {
		return TestFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(TestFunction function) {
		return null;
	}

	@Override
	public TestFunction newFunction() {
		return new TestFunction();
	}

	@Override
	public FileVersionId getHandlerPackage(TestFunction function) {
		// TODO Auto-generated method stub
		return super.getHandlerPackage(function);
	}

}
