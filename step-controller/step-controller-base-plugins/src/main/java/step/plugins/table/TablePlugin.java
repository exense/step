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
package step.plugins.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.access.AuthorizationManager;
import step.framework.server.tables.TableRegistry;

@Plugin(dependencies = {ObjectHookControllerPlugin.class})
public class TablePlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TablePlugin.class);

    @Override
    public void serverStart(GlobalContext context) {
        context.getServiceRegistrationCallback().registerService(TableService.class);
        TableRegistry tableRegistry = context.require(TableRegistry.class);
        ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
        AuthorizationManager authorizationManager = context.require(AuthorizationManager.class);
        Integer maxRequestDuration = context.getConfiguration().getPropertyAsInteger("table.query.maxTime");
        if (maxRequestDuration == null) {
            maxRequestDuration = context.getConfiguration().getPropertyAsInteger("db.query.maxTime", 30);
            logger.warn("The Step property db.query.maxTime is deprecated, use table.query.maxTime instead");
        }
        Integer maxResultCount = context.getConfiguration().getPropertyAsInteger("table.query.maxResultCount");
        if (maxResultCount == null) {
            maxResultCount = context.getConfiguration().getPropertyAsInteger("db.query.maxCount", 1000);
            logger.warn("The Step property db.query.maxCount is deprecated, use table.query.maxResultCount instead");
        }
        step.framework.server.tables.service.TableService tableService = new step.framework.server.tables.service.TableService(tableRegistry, objectHookRegistry, authorizationManager, maxRequestDuration, maxResultCount);
        context.put(step.framework.server.tables.service.TableService.class, tableService);
    }

}
