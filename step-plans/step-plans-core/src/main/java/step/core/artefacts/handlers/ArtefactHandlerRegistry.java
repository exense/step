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
package step.core.artefacts.handlers;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

public class ArtefactHandlerRegistry {

	private final Map<Class<? extends AbstractArtefact>, Class<? extends ArtefactHandler<?, ?>>> register = new ConcurrentHashMap<>();
	private final Map<String, Class<? extends AbstractArtefact>> artefactClassesByName = new ConcurrentHashMap<>();

	public Class<? extends ArtefactHandler<?, ?>> get(Class<? extends AbstractArtefact> key) {
		return register.get(key);
	}

	public Class<? extends ArtefactHandler<?, ?>> put(Class<? extends AbstractArtefact> key,
			Class<? extends ArtefactHandler<?, ?>> value) {
		artefactClassesByName.put(AbstractArtefact.getArtefactName(key), key);
		return register.put(key, value);
	}

	public Set<String> getArtefactNames() {
		return artefactClassesByName.keySet();
	}

	public Class<? extends AbstractArtefact> getArtefactType(String name) {
		return artefactClassesByName.get(name);
	}

	public AbstractArtefact getArtefactTypeInstance(String type) throws Exception {
		Class<? extends AbstractArtefact> clazz = getArtefactType(type);
		AbstractArtefact sample = clazz.newInstance();
		for (Method m : clazz.getMethods()) {
			if (m.getAnnotation(PostConstruct.class) != null) {
				m.invoke(sample);
			}
		}
		return sample;
	}

	public Set<String> getArtefactTemplateNames() {
		Set<String> templateArtefacts = new HashSet<String>();
		getArtefactNames().forEach(k -> {
			if (getArtefactType(k).getAnnotation(Artefact.class).useAsTemplate()) {
				templateArtefacts.add(k);
			}
		});
		return templateArtefacts;
	}
}
