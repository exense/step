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
package step.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryResourceRevisionAccessor extends InMemoryCRUDAccessor<ResourceRevision>
		implements ResourceRevisionAccessor {

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByResourceId(String resourceId) {
		List<ResourceRevision> result = new ArrayList<>();
		getAll().forEachRemaining(r->{
			if(r.getResourceId().equals(resourceId)) {
				result.add(r);
			}
		});;
		return result.iterator();
	}

	@Override
	public Iterator<ResourceRevision> getResourceRevisionsByChecksum(String checksum) {
		List<ResourceRevision> result = new ArrayList<>();
		getAll().forEachRemaining(r->{
			if(r.getChecksum().equals(checksum)) {
				result.add(r);
			}
		});;
		return result.iterator();
	}

}
