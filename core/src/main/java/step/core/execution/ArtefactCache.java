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
package step.core.execution;

import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;

public class ArtefactCache {
	
	private ConcurrentHashMap<String,AbstractArtefact> cache = new ConcurrentHashMap<>();
	
	public void remove(ReportNode node) {
		cache.remove(node.getId().toString());
	}
	
	public AbstractArtefact get(String nodeId) {
		// Disabling cache as it is causing concurrency issues with artefacts using DynamicValue
		return null;//cache.get(nodeId);
	}
	
	public void put(AbstractArtefact node) {
		cache.put(node.getId().toString(), node);
	}


}
