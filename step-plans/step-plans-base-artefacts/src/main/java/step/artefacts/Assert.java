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
package step.artefacts;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

@Artefact()
public class Assert extends AbstractArtefact {

	private DynamicValue<String> actual = new DynamicValue<>("");
	
	private AssertOperator operator;
	
	
	//private boolean negate = false;
	
	DynamicValue<Boolean> doNegate = new DynamicValue<Boolean>(false);

	
	private DynamicValue<?> expected = new DynamicValue<>("");
	
	private DynamicValue<String> customErrorMessage = new DynamicValue<>("");

	public DynamicValue<String> getCustomErrorMessage() {
		return customErrorMessage;
	}

	public void setCustomErrorMessage(DynamicValue<String> customErrorMessage) {
		this.customErrorMessage = customErrorMessage;
	}

	public Assert() {
		super();
	}
	
	public DynamicValue<String> getActual() {
		return actual;
	}

	public void setActual(DynamicValue<String> actual) {
		this.actual = actual;
	}

	public AssertOperator getOperator() {
		return operator;
	}

	public void setOperator(AssertOperator operator) {
		this.operator = operator;
	}

	public DynamicValue<?> getExpected() {
		return expected;
	}

	public void setExpected(DynamicValue<?> expected) {
		this.expected = expected;
	}

	
/*	public boolean isNegate() {
		return negate;
	}

	public void setNegate(boolean negate) {
		this.negate = negate;
	}*/

	public DynamicValue<Boolean> getDoNegate() {
		return doNegate;
	}

	public void setDoNegate(DynamicValue<Boolean> doNegate) {
		this.doNegate = doNegate;
	}



	public enum AssertOperator {
		
		EQUALS,
		
		BEGINS_WITH,
		
		ENDS_WITH,
		
		CONTAINS,

		MATCHES,

		LESS_THAN,

		LESS_THAN_OR_EQUALS,

		GREATER_THAN,

		GREATER_THAN_OR_EQUALS,

		IS_NULL;
	}

}
