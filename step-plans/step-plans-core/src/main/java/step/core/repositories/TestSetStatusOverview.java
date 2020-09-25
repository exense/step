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
package step.core.repositories;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TestSetStatusOverview implements Serializable {

	String testsetName;
	
	List<TestRunStatus> runs = new ArrayList<>();

	public TestSetStatusOverview() {
		super();
	}
	
	public TestSetStatusOverview(String testsetName) {
		super();
		this.testsetName = testsetName;
	}

	public String getTestsetName() {
		return testsetName;
	}

	public void setTestsetName(String testsetName) {
		this.testsetName = testsetName;
	}

	public List<TestRunStatus> getRuns() {
		return runs;
	}

	public void setRuns(List<TestRunStatus> runs) {
		this.runs = runs;
	}
}
