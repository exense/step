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

import jakarta.annotation.PostConstruct;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

	/**
	 * @return the list of artefacts that can be used as control within Plans
	 */
	public Set<String> getControlArtefactNames() {
		return artefactClassesByName.keySet().stream()
				.filter(a -> getArtefactType(a).getAnnotation(Artefact.class).validAsControl()).collect(Collectors.toSet());
	}

	/**
	 * @return the list of artefacts that can be used as root element of Plans
	 */
	public Set<String> getRootArtefactNames() {
		return artefactClassesByName.keySet().stream().filter(k -> getArtefactType(k).getAnnotation(Artefact.class).validAsRoot())
				.collect(Collectors.toSet());
	}

	public ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(Class<AbstractArtefact> artefactClass) {
		// Be careful not to cache the ArtefactHandlerRegistry as this is a mutable variable of the context...
		Artefact artefact = artefactClass.getAnnotation(Artefact.class);
		if(artefact!=null) {
			Class<ArtefactHandler<AbstractArtefact, ReportNode>> artefactHandlerClass;
			artefactHandlerClass = (Class<ArtefactHandler<AbstractArtefact, ReportNode>>) get(artefactClass);
			if(artefactHandlerClass!=null) {
				ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler;
				try {
					artefactHandler = artefactHandlerClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException("Unable to instantiate artefact handler for the artefact class " + artefactClass, e);
				}

				return artefactHandler;
			} else {
				throw new RuntimeException("No artefact handler found for the artefact class " + artefactClass);
			}
		} else {
			throw new RuntimeException("The class " + artefactClass + " is not annotated as artefact!");
		}
	}
}
