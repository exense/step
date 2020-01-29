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
package step.core.artefacts;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.reflections.Reflections;

public class ArtefactRegistry {
	
	private static ArtefactRegistry instance;

	@SuppressWarnings("unchecked")
	public static synchronized ArtefactRegistry getInstance() {
		if(instance == null) {
			instance = new ArtefactRegistry();
			
			Set<Class<?>> artefacts = new Reflections("step.artefacts").getTypesAnnotatedWith(Artefact.class);
			
			for(Class<?> artfefactClass:artefacts) {
				instance.register((Class<? extends AbstractArtefact>) artfefactClass);
			}
		}
		
		return instance;
	}

	Map<String, Class<? extends AbstractArtefact>> register = new HashMap<>();
	
	public void register(Class<? extends AbstractArtefact> artefact) {
		register.put(getArtefactName(artefact), artefact);
	}
	
	public Set<String> getArtefactNames() {
		return register.keySet();
	}
	
	public Class<? extends AbstractArtefact> getArtefactType(String name) {
		return register.get(name);
	}

	public AbstractArtefact getArtefactTypeInstance(String type) throws Exception {
		Class<? extends AbstractArtefact> clazz = getInstance().getArtefactType(type);		
		AbstractArtefact sample = clazz.newInstance();
		for(Method m:clazz.getMethods()) {
			if(m.getAnnotation(PostConstruct.class)!=null) {
				m.invoke(sample);
			}
		}
		
		return sample;
	}

	public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		Artefact annotation = artefactClass.getAnnotation(Artefact.class);
		return annotation.name().length()>0?annotation.name():artefactClass.getSimpleName();
	}
	
}
