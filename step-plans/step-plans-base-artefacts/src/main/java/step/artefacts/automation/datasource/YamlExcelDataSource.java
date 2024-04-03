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
import step.datapool.excel.ExcelDataPool;
import step.plans.parser.yaml.model.YamlResourceReference;

public class YamlExcelDataSource extends AbstractYamlDataSource<ExcelDataPool> {

    protected YamlResourceReference file = new YamlResourceReference();

    protected DynamicValue<String> worksheet = new DynamicValue<>();

    protected DynamicValue<Boolean> headers = new DynamicValue<>(true);

    public YamlExcelDataSource() {
        super("excel");
    }

    @Override
    public void fillDataPoolConfiguration(ExcelDataPool res) {
        super.fillDataPoolConfiguration(res);
        if (file != null) {
            res.setFile(file.toDynamicValue());
        }
        if (worksheet != null) {
            res.setWorksheet(worksheet);
        }
        if (headers != null) {
            res.setHeaders(headers);
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(ExcelDataPool dataPoolConfiguration, boolean isForWriteEditable) {
        super.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
        if (dataPoolConfiguration.getFile() != null) {
            this.file = YamlResourceReference.fromDynamicValue(dataPoolConfiguration.getFile());
        }
        if (dataPoolConfiguration.getHeaders() != null) {
            this.headers = dataPoolConfiguration.getHeaders();
        }
        if (dataPoolConfiguration.getWorksheet() != null) {
            this.worksheet = dataPoolConfiguration.getWorksheet();
        }
    }
}
