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
package step.plugins.screentemplating;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ch.exense.commons.core.accessors.AbstractAccessor;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Filters;

public class ScreenInputAccessorImpl extends AbstractAccessor<ScreenInput> implements ScreenInputAccessor {

	public ScreenInputAccessorImpl(Collection<ScreenInput> collectionDriver) {
		super(collectionDriver);
	}

	@Override
	public List<ScreenInput> getScreenInputsByScreenId(String screenId) {
		return collectionDriver
				.find(Filters.equals("screenId", screenId), null, null, null, 0).sorted(Comparator
						.comparingInt(ScreenInput::getPosition).thenComparing(Comparator.comparing(ScreenInput::getId)))
				.collect(Collectors.toList());
	}

}
