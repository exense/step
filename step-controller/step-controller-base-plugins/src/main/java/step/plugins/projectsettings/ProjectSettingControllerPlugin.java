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
package step.plugins.projectsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.encryption.EncryptionManager;
import step.core.entities.Entity;
import step.core.objectenricher.*;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.encryption.AbstractEncryptedValuesManager;
import step.plugins.encryption.EncryptionManagerDependencyPlugin;
import step.plugins.encryption.EncryptedEntityImportBiConsumer;
import step.plugins.encryption.EncryptedEntityExportBiConsumer;
import step.plugins.screentemplating.ScreenTemplatePlugin;
import step.projectsettings.ProjectSetting;
import step.projectsettings.ProjectSettingAccessor;
import step.projectsettings.ProjectSettingAccessorImpl;
import step.projectsettings.ProjectSettingManager;

import java.util.Set;
import java.util.stream.Collectors;

import static step.core.EncryptedTrackedObject.PARAMETER_PROTECTED_VALUE_FIELD;
import static step.core.EncryptedTrackedObject.PARAMETER_VALUE_FIELD;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ScreenTemplatePlugin.class, EncryptionManagerDependencyPlugin.class})
public class ProjectSettingControllerPlugin extends AbstractControllerPlugin {

    public static Logger logger = LoggerFactory.getLogger(ProjectSettingControllerPlugin.class);

    private ProjectSettingManager projectSettingManager;
    private EncryptionManager encryptionManager;

    @Override
    public void serverStart(GlobalContext context) {
        // The encryption manager might be null
        encryptionManager = context.get(EncryptionManager.class);

        Collection<ProjectSetting> collection = context.getCollectionFactory().getCollection(ProjectSetting.ENTITY_NAME, ProjectSetting.class);

        ProjectSettingAccessor projectSettingAccessor = new ProjectSettingAccessorImpl(collection);
        context.put(ProjectSettingAccessor.class, projectSettingAccessor);

        ProjectSettingManager projectSettingManager = new ProjectSettingManager(projectSettingAccessor, encryptionManager, context.getConfiguration());
        context.put(ProjectSettingManager.class, projectSettingManager);
        this.projectSettingManager = projectSettingManager;

        context.getEntityManager().register(new Entity<>(
                ProjectSetting.ENTITY_NAME,
                projectSettingAccessor,
                ProjectSetting.class));
        context.getEntityManager().registerExportHook(new EncryptedEntityExportBiConsumer<ProjectSetting>(ProjectSetting.class, projectSettingManager.getEntityNameForLogging()) {
            @Override
            protected Object getValue(ProjectSetting obj) {
                return obj.getValue();
            }

            @Override
            protected void setResetValue(ProjectSetting obj) {
                obj.setValue(AbstractEncryptedValuesManager.RESET_VALUE);
            }
        });
        context.getEntityManager().registerImportHook(new EncryptedEntityImportBiConsumer<ProjectSetting>(encryptionManager, ProjectSetting.class, projectSettingManager.getEntityNameForLogging()) {
            @Override
            protected Object getValue(ProjectSetting obj) {
                return obj.getValue();
            }

            @Override
            protected void setResetValue(ProjectSetting obj) {
                obj.setValue(AbstractEncryptedValuesManager.RESET_VALUE);
            }
        });

        context.getServiceRegistrationCallback().registerService(ProjectSettingServices.class);
    }

    @Override
    public void initializeData(GlobalContext context) throws Exception {

        if(encryptionManager != null) {
            if(encryptionManager.isFirstStart()) {
                logger.info("First start of the encryption manager. Encrypting all protected parameters...");
                projectSettingManager.encryptAll();
            }
            if(encryptionManager.isKeyPairChanged()) {
                logger.info("Key pair of encryption manager changed. Resetting all protected parameters...");
                projectSettingManager.resetAllProtectedValues();
            }
        }
    }

}
