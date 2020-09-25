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

import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.grid.filemanager.FileVersionId;

public class TestFunctionType extends AbstractFunctionType<TestFunction> {

	@Override
	public String getHandlerChain(TestFunction function) {
		return TestFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(TestFunction function) {
		return null;
	}

	@Override
	public TestFunction newFunction() {
		return new TestFunction();
	}

	@Override
	public FileVersionId getHandlerPackage(TestFunction function) {
		// TODO Auto-generated method stub
		return super.getHandlerPackage(function);
	}

}
