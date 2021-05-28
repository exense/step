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
package step.core.tables;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import step.core.collections.Filter;
import step.core.collections.Filters;

public class MultiTextCriterium implements TableColumnSearchQueryFactory {

	private final List<String> attributes;

	public MultiTextCriterium(List<String> attributes) {
		super();
		this.attributes = attributes;
	}

	public MultiTextCriterium(String... attributes) {
		super();
		this.attributes = Arrays.asList(attributes);
	}

	@Override
	public Filter createQuery(String attributeName, String expression) {
		return Filters
				.or(attributes.stream().map(a -> Filters.regex(a, expression, false)).collect(Collectors.toList()));
	}

}
