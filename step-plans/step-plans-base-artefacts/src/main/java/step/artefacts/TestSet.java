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
import step.core.yaml.YamlModel;

@YamlModel(name = "testSet")
@Artefact(name = "TestSet", validAsRoot = true)
public class TestSet extends AbstractArtefact {

	private DynamicValue<Integer> threads = new DynamicValue<Integer>(1);
	
	public TestSet() {
		super();
	}

	public TestSet(int threads) {
		this();
		this.threads = new DynamicValue<Integer>(threads);
	}
	
	public boolean isCreateSkeleton() {
		return true;
	}

	public DynamicValue<Integer> getThreads() {
		return threads;
	}

	public void setThreads(DynamicValue<Integer> threads) {
		this.threads = threads;
	}

}
