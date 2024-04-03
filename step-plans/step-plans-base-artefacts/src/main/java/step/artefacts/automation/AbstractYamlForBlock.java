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

import step.artefacts.AbstractForBlock;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.model.AbstractYamlArtefact;

public class AbstractYamlForBlock<T extends AbstractForBlock> extends AbstractYamlArtefact<T> {
    protected DynamicValue<String> item = null;

    protected DynamicValue<Integer> maxFailedLoops = null;

    protected DynamicValue<Integer> threads = null;

    protected DynamicValue<String> globalCounter = null;

    protected DynamicValue<String> userItem = null;

    public AbstractYamlForBlock() {
    }

    protected AbstractYamlForBlock(Class<T> artefactClass) {
        super(artefactClass);
    }

    @Override
    protected void fillArtefactFields(T res) {
        super.fillArtefactFields(res);
        if (item != null) {
            res.setItem(item);
        }
        if (maxFailedLoops != null) {
            res.setMaxFailedLoops(maxFailedLoops);
        }
        if (threads != null) {
            res.setThreads(threads);
        }
        if (globalCounter != null) {
            res.setGlobalCounter(globalCounter);
        }
        if (userItem != null) {
            res.setUserItem(userItem);
        }
    }

    @Override
    protected void fillYamlArtefactFields(T artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getItem() != null) {
            this.item = artefact.getItem();
        }
        if (artefact.getMaxFailedLoops() != null) {
            this.maxFailedLoops = artefact.getMaxFailedLoops();
        }
        if (artefact.getThreads() != null) {
            this.threads = artefact.getThreads();
        }
        if (artefact.getGlobalCounter() != null) {
            this.globalCounter = artefact.getGlobalCounter();
        }
        if (artefact.getUserItem() != null) {
            this.userItem = artefact.getUserItem();
        }
    }

    @Override
    protected T createArtefactInstance() {
        T res = super.createArtefactInstance();
        res.init();
        return res;
    }
}
