package step.repositories.parser.annotated;

import step.artefacts.Check;
import step.core.dynamicbeans.DynamicValue;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.steps.ExpectedStep;

public class DefaultExpectedStepParser extends AnnotatedStepParser<ExpectedStep> {

	public DefaultExpectedStepParser() {
		super(ExpectedStep.class);
		score = 100;
	}

	@Step(value = "(.+?)[ ]+and[ ]+(.+?)", priority = 10)
	public static void and(ParsingContext parsingContext, String left, String right) {
		ExpectedStep leftStep = new ExpectedStep(left);
		ExpectedStep rightStep = new ExpectedStep(right);
		parsingContext.parseStep(leftStep);
		parsingContext.parseStep(rightStep);
	}
	
	@Step("(.+?)[ ]+is[ ]+(.+?)")
	public static void map(ParsingContext parsingContext, String key, String value) {
		Check result = new Check();
		result.setExpression(new DynamicValue<>("'"+value+"'.equals(output.getString('"+key+"'))",""));
		parsingContext.addArtefactToCurrentParent(result);
	}
	
}
