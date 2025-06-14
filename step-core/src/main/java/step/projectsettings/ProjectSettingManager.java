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
import step.core.encryption.EncryptionManager;
import step.encryption.AbstractEncryptedValuesManager;

import java.util.*;

public class ProjectSettingManager extends AbstractEncryptedValuesManager<ProjectSetting, String> {

    private final ProjectSettingAccessor accessor;

    public ProjectSettingManager(ProjectSettingAccessor accessor, EncryptionManager encryptionManager, Configuration configuration) {
        this(accessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE));
    }

    public ProjectSettingManager(ProjectSettingAccessor accessor, EncryptionManager encryptionManager, String defaultScriptEngine) {
        // project settings can't be dynamic, so the dynamic bean resolver is null
        super(encryptionManager, defaultScriptEngine, null);
        this.accessor = accessor;
    }

    public static ProjectSetting maskProtectedValue(ProjectSetting obj) {
        if(obj != null && isProtected(obj) & !RESET_VALUE.equals(obj.getValue())) {
            obj.setValue(PROTECTED_VALUE);
        }
        return obj;
    }

    @Override
    protected Accessor<ProjectSetting> getAccessor() {
        return accessor;
    }

    @Override
    protected boolean isDynamicValue(ProjectSetting obj) {
        return false;
    }

    @Override
    protected String getStringValue(ProjectSetting obj) {
        return obj.getValue();
    }

    @Override
    protected void setValue(ProjectSetting obj, String value) {
        obj.setValue(value);
    }

    @Override
    protected String getValue(ProjectSetting obj) {
        return obj.getValue();
    }

    @Override
    public String getResetValue() {
        return RESET_VALUE;
    }

    @Override
    public String getEntityNameForLogging() {
        return "project setting";
    }

    public List<ProjectSetting> getAllSettingsWithUniqueKeys() {
        return accessor.getSettingsWithHighestPriority();
    }

    public ProjectSetting getUniqueSettingByKey(String key) {
        return accessor.getSettingWithHighestPriority(key);
    }
}
