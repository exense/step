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
package step.core.dynamicbeans;

import ch.exense.commons.core.model.dynamicbeans.ContainsDynamicValues;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;

public class TestBean {

	DynamicValue<String> testString = new DynamicValue<>("'test'", "js");
	
	DynamicValue<Boolean> testBoolean = new DynamicValue<>("true", "js");
	
	DynamicValue<Integer> testInteger = new DynamicValue<>("10", "js");
	
	DynamicValue<TestBean2> testRecursive = new DynamicValue<>("new step.core.dynamicbeans.TestBean2()", "js");

	TestBean2[] testArray = new TestBean2[]{new TestBean2(), new TestBean2()};
	
	TestBean2 testRecursive2 = new TestBean2();

	public DynamicValue<String> getTestString() {
		return testString;
	}

	public DynamicValue<Boolean> getTestBoolean() {
		return testBoolean;
	}

	public DynamicValue<Integer> getTestInteger() {
		return testInteger;
	}

	public DynamicValue<TestBean2> getTestRecursive() {
		return testRecursive;
	}

	public void setTestString(DynamicValue<String> testString) {
		this.testString = testString;
	}

	public void setTestBoolean(DynamicValue<Boolean> testBoolean) {
		this.testBoolean = testBoolean;
	}

	public void setTestInteger(DynamicValue<Integer> testInteger) {
		this.testInteger = testInteger;
	}

	public void setTestRecursive(DynamicValue<TestBean2> testRecursive) {
		this.testRecursive = testRecursive;
	}

	@ContainsDynamicValues
	public TestBean2 getTestRecursive2() {
		return testRecursive2;
	}

	public void setTestRecursive2(TestBean2 testRecursive2) {
		this.testRecursive2 = testRecursive2;
	}

	public TestBean2[] getTestArray() {
		return testArray;
	}

	public void setTestArray(TestBean2[] testArray) {
		this.testArray = testArray;
	}
	
}
