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
import step.core.access.Role;
import step.core.access.RoleResolver;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.Session;
import step.framework.server.access.AccessManager;
import step.framework.server.tables.TableRegistry;

import java.util.Objects;

@Plugin(dependencies = {ObjectHookControllerPlugin.class})
public class TablePlugin extends AbstractControllerPlugin {

    private TableRegistry tableRegistry;
    private ObjectHookRegistry objectHookRegistry;
    private Integer maxRequestDuration;
    private Integer maxResultCount;

    @Override
    public void serverStart(GlobalContext context) {
        context.getServiceRegistrationCallback().registerService(TableService.class);
        tableRegistry = context.require(TableRegistry.class);
        objectHookRegistry = context.require(ObjectHookRegistry.class);
        maxRequestDuration = context.getConfiguration().getPropertyAsInteger("db.query.maxTime", 30);
        maxResultCount = context.getConfiguration().getPropertyAsInteger("db.query.maxCount", 1000);
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);
        //Security plugin is part of enterprise edition.
        AccessManager accessManager = Objects.requireNonNullElse(context.get(AccessManager.class), new NoAccessManager());

        step.framework.server.tables.service.TableService tableService = new step.framework.server.tables.service.TableService(tableRegistry, objectHookRegistry, accessManager, maxRequestDuration, maxResultCount);
        context.put(step.framework.server.tables.service.TableService.class, tableService);
    }

    private class NoAccessManager implements AccessManager {
        @Override
        public void setRoleResolver(RoleResolver roleResolver) {

        }

        @Override
        public boolean checkRightInContext(Session session, String s) {
            return true;
        }

        @Override
        public Role getRoleInContext(Session session) {
            return null;
        }
    }
}
