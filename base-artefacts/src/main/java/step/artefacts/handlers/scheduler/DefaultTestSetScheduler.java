/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers.scheduler;

import java.util.ArrayList;
import java.util.List;

import step.core.artefacts.AbstractArtefact;

public class DefaultTestSetScheduler extends TestSetScheduler {
	
	@Override
	public List<TestCaseBundle> bundleTestCases(List<AbstractArtefact> artefacts, int numberOfBundles) {
		
		List<TestCaseBundle> bundles = new ArrayList<TestCaseBundle>(numberOfBundles);
		for(int i=0;i<numberOfBundles;i++) {
			bundles.add(new TestCaseBundle());
		}
		
		int currentBundle = 0;
		for(AbstractArtefact artefact:artefacts) {
			bundles.get(currentBundle).getTestcases().add(artefact);
			currentBundle=(currentBundle+1)%numberOfBundles;					
		}
		
		return bundles;
	}

}
