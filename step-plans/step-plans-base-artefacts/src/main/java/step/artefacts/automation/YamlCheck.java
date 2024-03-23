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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.Check;
import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.AbstractYamlArtefact;

@AutomationPackageNamedEntity(name = "check")
public class YamlCheck extends AbstractYamlArtefact<Check> {

    private static final Logger log = LoggerFactory.getLogger(YamlCheck.class);

    private String expression = null;

    @Override
    protected void fillArtefactFields(Check res) {
        super.fillArtefactFields(res);
        if(getExpression() != null) {
            res.setExpression(new DynamicValue<>(getExpression(), ""));
        }
    }

    @Override
    protected void fillYamlArtefactFields(AbstractArtefact artefact) {
        super.fillYamlArtefactFields(artefact);
        Check checkArtefact = (Check) artefact;
        if (checkArtefact.getExpression() != null) {
            if (!checkArtefact.getExpression().isDynamic()) {
                log.warn("Static values are not supported in yaml plan format for 'expression' in " + artefact.getClass().getSimpleName());
            } else {
                this.setExpression(checkArtefact.getExpression().getExpression());
            }
        }
    }

    @Override
    protected Check createArtefactInstance() {
        return new Check();
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
