package step.functions.handler;

import step.functions.io.Input;
import step.functions.io.Output;

public class SecondTestFunctionHandler extends AbstractFunctionHandler<TestInput, TestOutput> {

	@Override
	protected Output<TestOutput> handle(Input<TestInput> input) throws Exception {
		Output<TestOutput> output = new Output<>();
		output.setPayload(new TestOutput("Bonjour"));
		return output;
	}

	@Override
	public Class<TestInput> getInputPayloadClass() {
		return TestInput.class;
	}

	@Override
	public Class<TestOutput> getOutputPayloadClass() {
		return TestOutput.class;
	}
}
