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
import step.datapool.gsheet.GoogleSheetv4DataPoolConfiguration;
import step.plans.parser.yaml.model.YamlResourceReference;

public class YamlGSheetDataSource extends AbstractYamlDataSource<GoogleSheetv4DataPoolConfiguration> {

    protected DynamicValue<String> fileId = new DynamicValue<String>("");
    protected YamlResourceReference serviceAccountKey = new YamlResourceReference();
    protected DynamicValue<String> tabName = new DynamicValue<String>("");

    public YamlGSheetDataSource() {
        super("gsheet");
    }

    @Override
    public void fillDataPoolConfiguration(GoogleSheetv4DataPoolConfiguration res) {
        super.fillDataPoolConfiguration(res);
        if (fileId != null) {
            res.setFileId(fileId);
        }
        if (tabName != null) {
            res.setTabName(tabName);
        }
        if (serviceAccountKey != null) {
            res.setServiceAccountKey(serviceAccountKey.toDynamicValue());
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(GoogleSheetv4DataPoolConfiguration dataPoolConfiguration, boolean isForWriteEditable) {
        super.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
        if (dataPoolConfiguration.getFileId() != null) {
            this.fileId = dataPoolConfiguration.getFileId();
        }
        if (dataPoolConfiguration.getTabName() != null) {
            this.tabName = dataPoolConfiguration.getTabName();
        }
        if (dataPoolConfiguration.getServiceAccountKey() != null) {
            this.serviceAccountKey = YamlResourceReference.fromDynamicValue(dataPoolConfiguration.getServiceAccountKey());
        }
    }
}
