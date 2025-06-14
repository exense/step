/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.projectsettings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.settings.AbstractSettingAccessorWithHook;
import step.unique.EntityWithUniqueAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ProjectSettingAccessorImpl extends AbstractSettingAccessorWithHook<ProjectSetting> implements ProjectSettingAccessor {

    public ProjectSettingAccessorImpl(Collection<ProjectSetting> collectionDriver) {
        super(collectionDriver);
    }

    @Override
    public List<ProjectSetting> getSettingsWithHighestPriority() {
        Stream<ProjectSetting> allEntities = getCollectionDriver().find(Filters.empty(), null, null, null, 0);
        return filterSettingsByPriority(allEntities);
    }

    @Override
    public ProjectSetting getSettingWithHighestPriority(String key) {
        Stream<ProjectSetting> allEntities = getCollectionDriver().find(Filters.equals(ProjectSetting.KEY_FIELD_NAME, key), null, null, null, 0);
        List<ProjectSetting> filtered = filterSettingsByPriority(allEntities);
        if (filtered.size() == 0) {
            return null;
        } else if (filtered.size() > 1) {
            throw new RuntimeException("Non unique project setting is detected by key: " + key);
        } else {
            return filtered.get(0);
        }
    }

    private static List<ProjectSetting> filterSettingsByPriority(Stream<ProjectSetting> allEntities) {
        // to support multitenancy here we want to filter out settings (defined for global project) overridden in local project
        List<ProjectSetting> highestPriorityProjectSettings = new ArrayList<>();

        ListMultimap<Object, ProjectSetting> groupedByKey = ArrayListMultimap.create();
        allEntities.forEach(e -> {
            groupedByKey.put(e.getKey(), e);
        });

        for (Object key : groupedByKey.keys()) {
            List<ProjectSetting> settingsWithTheSameKey = groupedByKey.get(key);
            ProjectSetting settingWithHighestPriority = null;
            for (ProjectSetting projectSetting : settingsWithTheSameKey) {
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
            if (settingWithHighestPriority != null) {
                highestPriorityProjectSettings.add(settingWithHighestPriority);
            }
        }
        return highestPriorityProjectSettings;
    }
}
