package step.core.collections.inmemory;

import ch.exense.commons.app.Configuration;
import step.core.collections.AbstractCollectionTest;

public class InMemoryCollectionTest extends AbstractCollectionTest {

	public InMemoryCollectionTest() {
		super(new InMemoryCollectionFactory(new Configuration()));
	}

}
