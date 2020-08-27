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

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ArtefactTypeIdResolver implements TypeIdResolver {

	@Override
	public Id getMechanism() {
		return Id.CUSTOM;
	}

	@Override
	public String idFromBaseType() {
		return null;
	}

	@Override
	public String idFromValue(Object arg0) {
		return idFromClass(arg0.getClass());
	}
	
	@SuppressWarnings("unchecked")
	private String idFromClass(Class<?>c) {
		return ArtefactTypeCache.getArtefactName((Class<? extends AbstractArtefact>) c);
	}

	@Override
	public String idFromValueAndType(Object arg0, Class<?> arg1) {
		return idFromClass(arg1);
	}

	@Override
	public void init(JavaType arg0) {
	}

	@Override
	public JavaType typeFromId(DatabindContext arg0, String arg1) {
		Class<? extends AbstractArtefact> artefactClass = ArtefactTypeCache.getArtefactType(arg1);
		return TypeFactory.defaultInstance().constructType(artefactClass);
	}

	@Override
	public String getDescForKnownTypeIds() {
		// TODO Auto-generated method stub
		return null;
	}

}
