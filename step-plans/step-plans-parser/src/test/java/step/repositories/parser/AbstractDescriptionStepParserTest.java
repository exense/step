package step.repositories.parser;

import step.repositories.parser.steps.DescriptionStep;

public class AbstractDescriptionStepParserTest extends AbstractStepParserTest {

	public AbstractDescriptionStepParserTest() {
		super();
	}

	protected DescriptionStep step(String input) {
		DescriptionStep step = new DescriptionStep();
		step.setValue(input);
		return step;
	}

}