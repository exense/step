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
package step.plans.parser.yaml.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import step.artefacts.DataSetArtefact;

public class DataSetRule extends SelectableDataSourceSupportRule {

    public DataSetRule(ObjectMapper stepYamlMapper) {
        super(stepYamlMapper);
    }

    protected boolean applicableClass(Class<?> artefactClass) {
        return DataSetArtefact.class.isAssignableFrom(artefactClass);
    }

    protected boolean applicableArtefactName(String artefactClass) {
        return artefactClass.equals(DataSetArtefact.DATA_SET_ARTIFACT_NAME);
    }

    @Override
    protected boolean isForWriteEditable() {
        return true;
    }

}
