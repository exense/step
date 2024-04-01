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
package step.artefacts.automation;

import com.google.common.collect.Lists;
import step.artefacts.*;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.jsonschema.JsonSchema;
import step.plans.parser.yaml.model.AbstractYamlArtefact;

import java.util.List;

public class YamlPerformanceAssert extends AbstractYamlArtefact<PerformanceAssert> {

    private DynamicValue<String> measurementName = new DynamicValue<>();
    private Aggregator aggregator = Aggregator.AVG;
    private Comparator comparator = Comparator.LOWER_THAN;

    @JsonSchema(defaultConstant = "3000")
    private DynamicValue<Number> expectedValue = new DynamicValue<Number>(3000l);

    public YamlPerformanceAssert() {
        super(PerformanceAssert.class);
    }

    @Override
    protected void fillArtefactFields(PerformanceAssert res) {
        super.fillArtefactFields(res);
        res.setAggregator(this.aggregator);
        res.setComparator(this.comparator);
        res.setExpectedValue(expectedValue);
        Filter filter = new Filter();
        filter.setField(new DynamicValue<>(AbstractOrganizableObject.NAME));
        filter.setFilterType(FilterType.EQUALS);
        filter.setFilter(this.measurementName);
        res.setFilters(Lists.newArrayList(filter));
    }

    @Override
    protected void fillYamlArtefactFields(PerformanceAssert artefact) {
        super.fillYamlArtefactFields(artefact);
        this.aggregator = artefact.getAggregator();
        this.comparator = artefact.getComparator();
        this.expectedValue = artefact.getExpectedValue();

        List<Filter> filters = artefact.getFilters();
        if (filters != null && !filters.isEmpty()) {
            if (filters.size() > 1) {
                throw new IllegalArgumentException("Multiple filters in " + artefact.getClass().getSimpleName() + " are not supported in yaml format");
            }
            Filter filter = filters.get(0);
            if (filter.getFilterType() != FilterType.EQUALS) {
                throw new IllegalArgumentException("Filter type " + filter.getFilterType() + " in " + artefact.getClass().getSimpleName() + " is not supported in yaml format");
            }
            if (!filter.getField().get().equals(AbstractOrganizableObject.NAME)) {
                throw new IllegalArgumentException("Filter field " + filter.getField() + " is not supported in yaml format.");
            }
            this.measurementName = filter.getFilter();
        }
    }
}
