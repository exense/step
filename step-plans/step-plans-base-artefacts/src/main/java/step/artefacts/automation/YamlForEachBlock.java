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

import step.artefacts.ForEachBlock;
import step.artefacts.automation.datasource.AbstractYamlDataSource;
import step.artefacts.automation.datasource.NamedYamlDataSource;
import step.core.yaml.YamlFieldCustomCopy;

public class YamlForEachBlock extends AbstractYamlForBlock<ForEachBlock> {

    @YamlFieldCustomCopy
    protected NamedYamlDataSource dataSource;

    public YamlForEachBlock() {
        super(ForEachBlock.class);
    }

    @Override
    protected void fillArtefactFields(ForEachBlock res) {
        super.fillArtefactFields(res);
        if (dataSource != null) {
            res.setDataSourceType(dataSource.getYamlDataSource().getDataSourceType());
            res.setDataSource(dataSource.getYamlDataSource().toDataPoolConfiguration(false));
        }
    }

    @Override
    protected void fillYamlArtefactFields(ForEachBlock artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getDataSource() != null) {
            this.dataSource = new NamedYamlDataSource(AbstractYamlDataSource.fromDataPoolConfiguration(artefact.getDataSource(), false));
        }
    }
}
