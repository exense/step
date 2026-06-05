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
package step.core.execution;


import step.core.artefacts.reports.ReportNodeAccessorImpl;
import step.core.collections.Filter;
import step.core.collections.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportNodesFilter {

    public ReportNodesFilter() {
        super();
    }

    public List<Filter> buildAdditionalQuery(ReportNodesTableParameters parameters) {
        List<Filter> fragments = new ArrayList<>();
        if (parameters != null) {
            String eid = parameters.getEid();
            if (eid != null) {
                fragments.add(Filters.equals(ReportNodeAccessorImpl.EXECUTION_ID_FIELD_NAME, eid));
            }

            List<String> ancestorIds = parameters.getAncestorIds();
            if (ancestorIds != null && !ancestorIds.isEmpty()) {
                if (ancestorIds.size() == 1) {
                    fragments.add(Filters.includes(ReportNodeAccessorImpl.ANCESTOR_IDS_FIELD_NAME, ancestorIds.getFirst()));
                } else {
                    fragments.add(Filters.or(ancestorIds.stream().map(aId -> Filters.includes(ReportNodeAccessorImpl.ANCESTOR_IDS_FIELD_NAME, aId)).collect(Collectors.toList())));
                }
            }
        }

        return fragments;
    }


}
