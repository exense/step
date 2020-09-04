package step.repositories.parser;

import java.util.List;

import org.junit.Assert;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.repositories.parser.StepsParser.ParsingException;

public class AbstractStepParserTest {

	protected StepsParser parser;
	
	@SuppressWarnings("unchecked")
	protected <T> T parseAndGetUniqueChild(List<AbstractStep> steps, Class<T> resultClass) throws ParsingException {
		Sequence root = new Sequence();
		parser.parseSteps(root, steps);
		List<AbstractArtefact> children = getChildren(root);
		Assert.assertEquals(1,children.size());
		return (T) children.get(0);
	}

	protected List<AbstractArtefact> getChildren(AbstractArtefact artefact) {
		return artefact.getChildren();
	}
	
	protected Sequence parse(List<AbstractStep> steps) throws ParsingException {
		Sequence root = new Sequence();
		parser.parseSteps(root, steps);
		return root;
	}

	public AbstractStepParserTest() {
		super();
	}

}