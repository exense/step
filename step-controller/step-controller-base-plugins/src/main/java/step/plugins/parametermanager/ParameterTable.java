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
package step.plugins.parametermanager;

import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.SearchOrder;
import step.framework.server.tables.AbstractTable;
import step.framework.server.tables.TableFindResult;
import step.parameter.Parameter;

import java.util.Iterator;

public class ParameterTable extends AbstractTable<Parameter> {

    public ParameterTable(Collection<Parameter> collection) {
        super(collection, "param-read", true);
    }

    @Override
    public TableFindResult<Parameter> find(Filter query, SearchOrder order, Integer skip, Integer limit) {
        TableFindResult<Parameter> find = super.find(query, order, skip, limit);

        Iterator<Parameter> iterator = find.getIterator();
        Iterator<Parameter> filteredIterator = new Iterator<Parameter>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Parameter next() {
                Parameter next = iterator.next();
                return ParameterServices.maskProtectedValue(next);
            }

        };
        TableFindResult<Parameter> filteredFind = new TableFindResult<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
        return filteredFind;
    }
}
