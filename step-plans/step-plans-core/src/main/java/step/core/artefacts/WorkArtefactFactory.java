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
package step.core.artefacts;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.handlers.ArtefactHandler;

public class WorkArtefactFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkArtefactFactory.class);
	
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		return createWorkArtefact(artefactClass, parentArtefact, name, copyChildren, true);
	}

	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren, boolean persistNode) {
		try {
			T artefact = artefactClass.newInstance();
			if(copyChildren) {
				// Property artefacts remain attached to their parent and are thus not subject to transclusion
				artefact.setChildren(ArtefactHandler.excludePropertyChildren(parentArtefact.getChildren()));
			}
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put("name", name);
			artefact.setAttributes(attributes);
			setPersistNodeValue(artefact, persistNode);
			return artefact;
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error("Error while creating new instance of "+artefactClass, e);
			return null;
		}
	}
	
	public void setPersistNodeValue(AbstractArtefact artefact, boolean persistNode) {
		artefact.setPersistNode(persistNode);
		for (AbstractArtefact child : artefact.getChildren()) {
			setPersistNodeValue(child, persistNode);
		}
	}

}
