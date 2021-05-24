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
package step.plugins.views;

import java.util.List;

import ch.exense.commons.core.accessors.AbstractAccessor;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Filters;

public class ViewModelAccessorImpl extends AbstractAccessor<ViewModel> implements ViewModelAccessor {

	public ViewModelAccessorImpl(Collection<ViewModel> collectionDriver) {
		super(collectionDriver);
		createOrUpdateIndex("executionId");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return (T) collectionDriver.find(
				Filters.and(List.of(Filters.equals("viewId", viewId), Filters.equals("executionId", executionId))),
				null, null, null, 0).findFirst().orElse(null);
	}
	
	@Override
	public void removeViewsByExecutionId(String executionId) {
		collectionDriver.remove(Filters.equals("executionId", executionId));
	}
}
