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
package step.plugins.unique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.collections.CollectionFactory;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.*;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.unique.UniqueEntityManager;

import java.util.Map;

@Plugin(dependencies= {ObjectHookControllerPlugin.class})
public class UniqueEntityControllerPlugin extends AbstractControllerPlugin {

    public static Logger logger = LoggerFactory.getLogger(UniqueEntityControllerPlugin.class);

    private CollectionFactory collectionFactory;

    @Override
    public void serverStart(GlobalContext context) {

        this.collectionFactory = context.getCollectionFactory();

        UniqueEntityManager uniqueEntityManager = new UniqueEntityManager();
        context.put(UniqueEntityManager.class, uniqueEntityManager);

        context.require(ObjectHookRegistry.class).add(new ObjectHook() {
            @Override
            public ObjectFilter getObjectFilter(AbstractContext abstractContext) {
                return null;
            }

            @Override
            public ObjectEnricher getObjectEnricher(AbstractContext abstractContext) {
                return null;
            }

            @Override
            public void rebuildContext(AbstractContext abstractContext, EnricheableObject enricheableObject) throws Exception {

            }

            @Override
            public boolean isObjectAcceptableInContext(AbstractContext abstractContext, EnricheableObject enricheableObject) {
                return true;
            }

            @Override
            public ObjectValidator getObjectValidator(AbstractContext context, Map<String, Object> config) {
                return uniqueEntityManager.createObjectValidator(collectionFactory, config);
            }
        });
    }

}
