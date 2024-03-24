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

import step.artefacts.Echo;
import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.YamlArtefact;

@YamlArtefact(forClass = Echo.class)
@AutomationPackageNamedEntity(name = "echo")
public class YamlEcho extends AbstractYamlArtefact<Echo> {

    private DynamicValue<String> text = new DynamicValue<>();

    public YamlEcho() {
        this.artefactClass = Echo.class;
    }

    @Override
    protected void fillArtefactFields(Echo res) {
        super.fillArtefactFields(res);
        DynamicValue<String> text = getText();
        if (text != null) {
            if (text.isDynamic()) {
                res.setText(new DynamicValue<>(text.getExpression(), text.getExpressionType()));
            } else {
                res.setText(new DynamicValue<>(text.getValue()));
            }
        }
    }

    @Override
    protected void fillYamlArtefactFields(Echo artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getText() != null) {
            DynamicValue<Object> echoText = artefact.getText();
            setText(echoText.isDynamic() ? new DynamicValue<>(echoText.getExpression(), echoText.getExpressionType()) : new DynamicValue<>((String) echoText.getValue()));
        }
    }

    public DynamicValue<String> getText() {
        return text;
    }

    public void setText(DynamicValue<String> text) {
        this.text = text;
    }
}
