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

import step.core.yaml.YamlFieldCustomCopy;
import step.datapool.file.FileDataPool;
import step.plans.parser.yaml.model.YamlResourceReference;

public class YamlFileDataSource<T extends FileDataPool> extends AbstractYamlDataSource<T> {

    @YamlFieldCustomCopy
    protected YamlResourceReference file = new YamlResourceReference();

    public YamlFileDataSource() {
        super("file");
    }

    protected YamlFileDataSource(String dataSourceType) {
        super(dataSourceType);
    }

    @Override
    public void fillDataPoolConfiguration(T res, boolean isForWriteEditable) {
        super.fillDataPoolConfiguration(res, isForWriteEditable);
        if (file != null) {
            res.setFile(file.toDynamicValue());
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(T dataPoolConfiguration, boolean isForWriteEditable) {
        super.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
        if (dataPoolConfiguration.getFile() != null) {
            this.file = YamlResourceReference.fromDynamicValue(dataPoolConfiguration.getFile());
        }
    }
}
