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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import step.core.scanner.CachedAnnotationScanner;

/**
 * The only reason why this class exists and has been implemented in a static
 * way is that {@link ArtefactTypeIdResolver} cannot be provided with a context
 * object. This class SHOULDN'T be used by any other class and should be removed
 * as soon as a better solution can be found.
 */
@SuppressWarnings("unchecked")
public class ArtefactTypeCache {
	
	private static final ArtefactTypeCache INSTANCE = new ArtefactTypeCache();
	
	private final Map<String, Class<? extends AbstractArtefact>> artefactRegister;
	
	/**
	 * Calling {@link AbstractArtefact#getArtefactName} is quite inefficient because it relies on
	 * {@link Class#getSimpleName()}. We're therefore using a cache for artefact names here
	 */
	private final Map<Class<? extends AbstractArtefact>, String> artefactNameCache;

	{
		artefactRegister = new ConcurrentHashMap<>();
		artefactNameCache = new ConcurrentHashMap<>();
		
		Set<Class<?>> artefactClasses = CachedAnnotationScanner.getClassesWithAnnotation(Artefact.class);
		for (Class<?> artefactClass_ : artefactClasses) {
			Class<? extends AbstractArtefact> artefactClass = (Class<? extends AbstractArtefact>) artefactClass_; 
			String artefactName = AbstractArtefact.getArtefactName((Class<AbstractArtefact>)artefactClass);
			artefactRegister.put(artefactName, artefactClass);
			artefactNameCache.put(artefactClass, artefactName);
		}
	}
	
	public static Class<? extends AbstractArtefact> getArtefactType(String name) {
		return INSTANCE.artefactRegister.get(name);
	}
	
	public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		return INSTANCE.artefactNameCache.get(artefactClass);
	}

	public static Set<String> keySet() {
		return INSTANCE.artefactRegister.keySet();
	}
}
