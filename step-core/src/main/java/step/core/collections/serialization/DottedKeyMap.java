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
package step.core.collections.serialization;

import java.util.HashMap;

/**
 * 
 * A special Map that is serialized by {@link DottedMapKeySerializer}
 * when persisted in the DB. This serializer supports the persistence of keys
 * that contain "." and "$" which are normally not allowed as key by Mongo.
 * 
 * 
 */
public class DottedKeyMap<K, V>  extends HashMap<K, V> {

	private static final long serialVersionUID = 8922169005470741941L;

}
