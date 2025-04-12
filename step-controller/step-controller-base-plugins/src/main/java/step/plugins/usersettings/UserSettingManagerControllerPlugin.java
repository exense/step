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
package step.plugins.usersettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.Accessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.encryption.EncryptionManager;
import step.core.entities.Entity;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.parameter.AbstractEncryptedValuesManager;
import step.plugins.encryption.EncryptionManagerDependencyPlugin;
import step.plugins.parametermanager.EncryptedEntityImportBiConsumer;
import step.plugins.parametermanager.EncyptedEntityExportBiConsumer;
import step.plugins.screentemplating.ScreenTemplatePlugin;
import step.usersettings.UserSetting;
import step.usersettings.UserSettingManager;

import java.util.Set;
import java.util.stream.Collectors;

import static step.core.EncryptedTrackedObject.PARAMETER_PROTECTED_VALUE_FIELD;
import static step.core.EncryptedTrackedObject.PARAMETER_VALUE_FIELD;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ScreenTemplatePlugin.class, EncryptionManagerDependencyPlugin.class})
public class UserSettingManagerControllerPlugin  extends AbstractControllerPlugin {

    public static Logger logger = LoggerFactory.getLogger(UserSettingManagerControllerPlugin.class);

    private UserSettingManager userSettingManager;
    private EncryptionManager encryptionManager;

    @Override
    public void serverStart(GlobalContext context) {
        // The encryption manager might be null
        encryptionManager = context.get(EncryptionManager.class);

        Collection<UserSetting> collection = context.getCollectionFactory().getCollection(UserSetting.ENTITY_NAME, UserSetting.class);

        Accessor<UserSetting> userSettingAccessor = new AbstractAccessor<>(collection);
        context.put("UserSettingAccessor", userSettingAccessor);

        context.get(TableRegistry.class).register(UserSetting.ENTITY_NAME, new Table<>(collection, "param-read", true)
                .withResultItemTransformer((p,s) -> AbstractEncryptedValuesManager.maskProtectedValue(p))
                .withDerivedTableFiltersFactory(lf -> {
                    Set<String> allFilterAttributes = lf.stream().map(Filters::collectFilterAttributes).flatMap(Set::stream).collect(Collectors.toSet());
                    return allFilterAttributes.contains(PARAMETER_VALUE_FIELD + ".value") ? new Equals(PARAMETER_PROTECTED_VALUE_FIELD, false) : Filters.empty();
                }));

        UserSettingManager userSettingManager = new UserSettingManager(userSettingAccessor, encryptionManager, context.getConfiguration(), context.getDynamicBeanResolver());
        context.put(UserSettingManager.class, userSettingManager);
        this.userSettingManager = userSettingManager;

        context.getEntityManager().register(new Entity<>(
                UserSetting.ENTITY_NAME,
                userSettingAccessor,
                UserSetting.class));
        context.getEntityManager().registerExportHook(new EncyptedEntityExportBiConsumer(UserSetting.class));
        context.getEntityManager().registerImportHook(new EncryptedEntityImportBiConsumer(encryptionManager, UserSetting.class));

        context.getServiceRegistrationCallback().registerService(UserSettingServices.class);
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {

        if(encryptionManager != null) {
            if(encryptionManager.isFirstStart()) {
                logger.info("First start of the encryption manager. Encrypting all protected parameters...");
                userSettingManager.encryptAllParameters();
            }
            if(encryptionManager.isKeyPairChanged()) {
                logger.info("Key pair of encryption manager changed. Resetting all protected parameters...");
                userSettingManager.resetAllProtectedParameters();
            }
        }
    }

}
