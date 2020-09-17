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
package step.plans.nl.parser;

import step.repositories.parser.AbstractStep;

public class PlanStep extends AbstractStep {

	String name;
	String dynamicNameExpression;
	
	String description;
	
	String command;
	
	String line;

	public PlanStep(String name, String dynamicNameExpression, String description, String command, String line) {
		super();
		this.name = name;
		this.dynamicNameExpression = dynamicNameExpression;
		this.description = description;
		this.command = command;
		this.line = line;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDynamicNameExpression() {
		return dynamicNameExpression;
	}
	
	public void setDynamicNameExpression(String dynamicNameExpression) {
		this.dynamicNameExpression = dynamicNameExpression;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}
}
