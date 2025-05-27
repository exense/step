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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import step.commons.activation.Activator;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.encryption.EncryptionManager;
import step.encryption.AbstractEncryptedValuesManager;
import step.unique.EntityWithUniqueAttributes;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    public String getEntityNameForLogging() {
        return "project setting";
    }

    public List<ProjectSetting> getAllSettingsWithUniqueKeys() {
        return getEntitiesWithHighestPriority().stream().map(e -> (ProjectSetting) e).collect(Collectors.toList());
    }

    private List<? extends EntityWithUniqueAttributes> getEntitiesWithHighestPriority() {
        // TODO: think if the ObjectFilter should also be applied here

        // TODO: maybe extract this logic to some common class (plugin)
        List<? extends EntityWithUniqueAttributes> allSettings = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(accessor.getAll(), Spliterator.ORDERED),
                false).map(e -> (EntityWithUniqueAttributes) e).collect(Collectors.toList());

        // to support multitenancy here we want to filter out settings (defined for global project) overridden in local project
        List<? extends EntityWithUniqueAttributes> highestPriorityProjectSettings = new ArrayList<>();
        ListMultimap<Object, EntityWithUniqueAttributes> groupedByKey = ArrayListMultimap.create();
        for (EntityWithUniqueAttributes setting : allSettings) {
            groupedByKey.put(setting.getKey(), setting);
        }

        for (Object key : groupedByKey.keys()) {
            List<EntityWithUniqueAttributes> settingsWithTheSameKey = groupedByKey.get(key);
            EntityWithUniqueAttributes settingWithHighestPriority = null;
            for (EntityWithUniqueAttributes projectSetting : settingsWithTheSameKey) {
                String otherPriority = ((AbstractOrganizableObject) projectSetting).getAttribute(EntityWithUniqueAttributes.ATTRIBUTE_PRIORITY);
                if (settingWithHighestPriority == null) {
                    settingWithHighestPriority = projectSetting;
                } else {
                    String currentPriority = ((AbstractOrganizableObject) settingWithHighestPriority).getAttribute(EntityWithUniqueAttributes.ATTRIBUTE_PRIORITY);
                    if (Objects.equals(currentPriority, otherPriority)) {
                        throw new RuntimeException("Validation failed. 2 setting with same keys " + key + " with various priorities have been detected");
                    } else if (currentPriority == null && otherPriority != null) {
                        settingWithHighestPriority = projectSetting;
                    } else if (otherPriority != null && Integer.parseInt(currentPriority) < Integer.parseInt(otherPriority)) {
                        settingWithHighestPriority = projectSetting;
                    }
                }
            }
        }
        return highestPriorityProjectSettings;
    }
}
