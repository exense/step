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
package step.artefacts.automation.datasource;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.json.JsonArrayDataPoolConfiguration;

public class YamlJsonArrayDataSource extends AbstractYamlDataSource<JsonArrayDataPoolConfiguration> {

    protected DynamicValue<String> json = new DynamicValue<String>("");

    public YamlJsonArrayDataSource() {
        super("json-array");
    }

    @Override
    public void fillDataPoolConfiguration(JsonArrayDataPoolConfiguration res) {
        super.fillDataPoolConfiguration(res);
        if (json != null) {
            res.setJson(json);
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(JsonArrayDataPoolConfiguration dataPoolConfiguration, boolean isForWriteEditable) {
        super.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
        if (dataPoolConfiguration.getJson() != null) {
            this.json = dataPoolConfiguration.getJson();
        }
    }
}
