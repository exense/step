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
package step.functions.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.functions.io.Output;
import step.functions.runner.FunctionRunner.Context;

public class FunctionRunnerTest {

	@Test
	public void test() throws IOException {
		TestFunction f = new TestFunction();
		f.setId(new ObjectId());
		f.setName("moustache");
		
		try(Context context = FunctionRunner.getContext(new TestFunctionType())) {
			Output<JsonObject> o = context.run(f, "{}");
			Assert.assertEquals("tache", o.getPayload().getString("mous"));
		}

	}
}
