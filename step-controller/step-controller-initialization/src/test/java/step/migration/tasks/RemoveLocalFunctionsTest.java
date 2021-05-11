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
package step.migration.tasks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.exense.commons.app.Configuration;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;

public class RemoveLocalFunctionsTest {

	private CollectionFactory collectionFactory;

	public RemoveLocalFunctionsTest() {
		super();
		
		collectionFactory = new InMemoryCollectionFactory(new Configuration());
	}

	@Test
	public void test() {
		Collection<Document> collection = collectionFactory.getCollection("functions", Document.class);
		Document document = new Document();
		document.put("type", "step.functions.base.types.LocalFunction");
		collection.save(document);
		new RemoveLocalFunctions(collectionFactory).runUpgradeScript();;
		assertEquals(0, collection.find(Filters.empty(), null, null, null, 0).count());		
	}

}
