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
package step.core.collections.filesystem;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.bson.types.ObjectId;

import step.core.accessors.AbstractIdentifiableObject;

public class AbstractCollection<T> {

	public AbstractCollection() {
		super();
	}

	protected ObjectId getId(T entity) {
		if (entity instanceof AbstractIdentifiableObject) {
			return ((AbstractIdentifiableObject) entity).getId();
		} else {
			Object idStr;
			try {
				idStr = PropertyUtils.getProperty(entity, AbstractIdentifiableObject.ID);
				return idStr != null ? new ObjectId(idStr.toString()) : null;
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected void setId(T entity, ObjectId id) {
		if (entity instanceof AbstractIdentifiableObject) {
			((AbstractIdentifiableObject) entity).setId(id);
		} else {
			try {
				PropertyUtils.setProperty(entity, AbstractIdentifiableObject.ID, id.toString());
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

}