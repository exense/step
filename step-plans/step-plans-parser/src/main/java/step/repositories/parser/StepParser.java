package step.repositories.parser;

public interface StepParser<T extends AbstractStep> {
	
	public int getParserScoreForStep(AbstractStep step);
	
	public void parseStep(ParsingContext parsingContext, T step);
	
}
