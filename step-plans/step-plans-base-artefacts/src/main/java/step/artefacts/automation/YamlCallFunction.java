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

import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFieldCustomCopy;

public class YamlCallFunction extends YamlTokenSelector<CallFunction> {

    protected DynamicValue<String> resultMap = new DynamicValue<>();

    @YamlFieldCustomCopy
    protected YamlDynamicInputs inputs = new YamlDynamicInputs("{}");

    @YamlFieldCustomCopy
    protected YamlKeywordDefinition keyword = new YamlKeywordDefinition(null,null, "{}");

    public YamlCallFunction() {
        super(CallFunction.class);
    }

    @Override
    protected void fillArtefactFields(CallFunction res) {
        super.fillArtefactFields(res);
        if (this.inputs != null) {
            res.setArgument(this.inputs.toDynamicValue());
        }
        if (this.keyword != null) {
            res.setFunction(this.keyword.toDynamicValue());
        }

        // for keywords, if nodeName is not defined or using dynamic name, we use the keyword name as default artefact name
        if (getNodeName() == null || getNodeName().isDynamic()) {
            String name;
            if (keyword != null && keyword.getKeywordName() != null && !keyword.getKeywordName().isEmpty()) {
                name = keyword.getKeywordName();
            } else {
                name = AbstractArtefact.getArtefactName(getArtefactClass());
            }
            res.addAttribute(AbstractOrganizableObject.NAME, name);
        }
    }

    @Override
    protected void fillYamlArtefactFields(CallFunction artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getArgument() != null) {
            this.inputs = YamlDynamicInputs.fromDynamicValue(artefact.getArgument());
        }
        if (artefact.getFunction() != null) {
            this.keyword = YamlKeywordDefinition.fromDynamicValue(artefact.getFunction());
        }

    }

    @Override
    protected String getDefaultNodeNameForYaml(CallFunction artefact) {
        if (artefact.getFunction() != null) {
            String keywordName = YamlKeywordDefinition.fromDynamicValue(artefact.getFunction()).getKeywordName();
            if (keywordName != null && !keywordName.isEmpty()) {
                return keywordName;
            }
        }
        return super.getDefaultNodeNameForYaml(artefact);
    }
}
