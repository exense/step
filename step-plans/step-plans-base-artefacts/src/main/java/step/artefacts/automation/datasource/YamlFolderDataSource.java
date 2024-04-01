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

import step.automation.packages.AutomationPackageNamedEntity;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.file.DirectoryDataPool;

@AutomationPackageNamedEntity(name = "folder")
public class YamlFolderDataSource extends AbstractYamlDataSource<DirectoryDataPool> {

    protected DynamicValue<String> folder = new DynamicValue<String>("");

    @Override
    public DirectoryDataPool createDataPoolConfiguration() {
        return new DirectoryDataPool();
    }

    @Override
    public void fillDataPoolConfiguration(DirectoryDataPool res) {
        if (folder != null) {
            res.setFolder(this.folder);
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(DirectoryDataPool dataPoolConfiguration) {
        if (dataPoolConfiguration.getFolder() != null) {
            this.folder = dataPoolConfiguration.getFolder();
        }
    }
}
