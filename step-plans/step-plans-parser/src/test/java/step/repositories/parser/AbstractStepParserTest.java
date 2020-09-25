/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
