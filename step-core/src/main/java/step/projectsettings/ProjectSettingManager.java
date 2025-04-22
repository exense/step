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
package step.projectsettings;

import ch.exense.commons.app.Configuration;
import step.commons.activation.Activator;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.encryption.EncryptionManager;
import step.encryption.AbstractEncryptedValuesManager;

public class ProjectSettingManager extends AbstractEncryptedValuesManager<ProjectSetting> {

    private final Accessor<ProjectSetting> accessor;

    public ProjectSettingManager(Accessor<ProjectSetting> accessor, EncryptionManager encryptionManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver) {
        this(accessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE), dynamicBeanResolver);
    }

    public ProjectSettingManager(Accessor<ProjectSetting> accessor, EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
        super(encryptionManager, defaultScriptEngine, dynamicBeanResolver);
        this.accessor = accessor;
    }

    @Override
    protected Accessor<ProjectSetting> getAccessor() {
        return accessor;
    }

    @Override
    protected String getEntityNameForLogging() {
        return "project setting";
    }
}
