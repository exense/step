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

import step.core.GlobalContext;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.access.AuthorizationManager;
import step.framework.server.tables.TableRegistry;

@Plugin(dependencies = {ObjectHookControllerPlugin.class})
public class TablePlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) {
        context.getServiceRegistrationCallback().registerService(TableService.class);
        TableRegistry tableRegistry = context.require(TableRegistry.class);
        ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
        AuthorizationManager authorizationManager = context.require(AuthorizationManager.class);
        Integer maxRequestDuration = context.getConfiguration().getPropertyAsInteger("db.query.maxTime", 30);
        Integer maxResultCount = context.getConfiguration().getPropertyAsInteger("db.query.maxCount", 1000);
        step.framework.server.tables.service.TableService tableService = new step.framework.server.tables.service.TableService(tableRegistry, objectHookRegistry, authorizationManager, maxRequestDuration, maxResultCount);
        context.put(step.framework.server.tables.service.TableService.class, tableService);
    }

}
