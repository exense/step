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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonObjectBuilder;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.core.json.JsonProviderCache;

public class ScreenInputAccessorImpl extends AbstractCRUDAccessor<ScreenInput> implements ScreenInputAccessor {

	public ScreenInputAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "screenInputs", ScreenInput.class);
	}

	@Override
	public List<ScreenInput> getScreenInputsByScreenId(String screenId) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		builder.add("screenId", screenId);

		List<ScreenInput> result = new ArrayList<>();
		String query = builder.build().toString();
		collection.find(query).as(ScreenInput.class).forEach(r->result.add(r));
		
		return result.stream().sorted(Comparator.comparingInt(ScreenInput::getPosition)
				.thenComparing(Comparator.comparing(ScreenInput::getId))).collect(Collectors.toList());
	}

}
