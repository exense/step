package step.plans.nl.parser;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.CallFunction;
import step.core.artefacts.AbstractArtefact;
import step.repositories.parser.AbstractStep;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.StepParser;
import step.repositories.parser.steps.DescriptionStep;
import step.repositories.parser.steps.ExpectedStep;

public class PlanStepParser implements StepParser<PlanStep> {

	@Override
	public int getParserScoreForStep(AbstractStep step) {
		return (step instanceof PlanStep)?1:0;
	}

	private static final String ASSERT_TOKEN = "Assert";
	
	@Override
	public void parseStep(ParsingContext parsingContext, PlanStep step) {
		AbstractStep subStep;
		if(step.command.trim().length()>0) {
			if(step.command.trim().startsWith(ASSERT_TOKEN)) {
				AbstractArtefact current = parsingContext.peekCurrentArtefact();
				subStep = new ExpectedStep(step.getCommand().replaceFirst(ASSERT_TOKEN, "").trim());
				// Expected block is parsed only if the current artefact is a CallFunction or if the last child of the current artefact is a CallFunction
				if(current!=null && current.getChildren()!=null && current.getChildren().size()>0) {
					AbstractArtefact lastChild = current.getChildren().get(current.getChildren().size()-1);
					if(lastChild instanceof CallFunction) {
						// the CallFunction is first pushed to the stack, so that the expected steps are added to the CallFunction as child
						parsingContext.pushArtefact(lastChild);				
						parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
						parsingContext.popCurrentArtefact();
					}
				} else if (current instanceof CallFunction) {
					parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
				}		
			} else {
				subStep = new DescriptionStep(step.getCommand().trim());			
				parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
			}
		}
	}
	

	private ParsingContext newParsingContext(ParsingContext parsingContext, PlanStep step) {
		return new ParsingContext(parsingContext) {

			@Override
			public void addArtefactToCurrentParent(AbstractArtefact artefact) {
				if(step.getName()!=null) {
					Map<String, String> attributes = new HashMap<>();
					attributes.put("name", step.getName());
					artefact.setAttributes(attributes);					
				}
				artefact.setAttachments(step.getAttachments());
				artefact.setDescription(step.getDescription());
				artefact.addCustomAttribute("line", step.line);
				artefact.addCustomAttribute("source", step.command);
				super.addArtefactToCurrentParent(artefact);
			}
			
			public void addParsingError(String errorMsg) {
				String errorPrefix = "Error on line "+step.line+ ": ";
				parsingErrors.add(new ParsingError(step, errorPrefix + errorMsg));
			}
			
		};
	}

}
