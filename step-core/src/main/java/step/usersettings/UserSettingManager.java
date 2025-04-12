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
package step.usersettings;

import ch.exense.commons.app.Configuration;
import step.commons.activation.Activator;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.encryption.EncryptionManager;
import step.parameter.AbstractEncryptedValuesManager;

public class UserSettingManager extends AbstractEncryptedValuesManager<UserSetting> {

    private final Accessor<UserSetting> userSettingAccessor;

    public UserSettingManager(Accessor<UserSetting> parameterAccessor, EncryptionManager encryptionManager, Configuration configuration, DynamicBeanResolver dynamicBeanResolver) {
        this(parameterAccessor, encryptionManager, configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE), dynamicBeanResolver);
    }

    public UserSettingManager(Accessor<UserSetting> userSettingAccessor, EncryptionManager encryptionManager, String defaultScriptEngine, DynamicBeanResolver dynamicBeanResolver) {
        super(encryptionManager, defaultScriptEngine, dynamicBeanResolver);
        this.userSettingAccessor = userSettingAccessor;
    }

    @Override
    protected Accessor<UserSetting> getAccessor() {
        return userSettingAccessor;
    }
}
