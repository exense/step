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

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.collections.inmemory.InMemoryCollection;
import step.unique.EntityWithUniqueAttributes;

import java.util.List;

public class ProjectSettingAccessorImplTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(ProjectSettingAccessorImplTest.class);

    @Test
    public void testGetSettingsWithHighestPriority(){
        ProjectSettingAccessorImpl accessor = new ProjectSettingAccessorImpl(new InMemoryCollection<>());
        prepareTestData(accessor);

        List<ProjectSetting> settingsWithHighestPriority = accessor.getSettingsWithHighestPriority(null);
        log.info("Found settings: {}", settingsWithHighestPriority);

        Assert.assertEquals(3, settingsWithHighestPriority.size());

        ProjectSetting s = settingsWithHighestPriority.stream().filter(ss -> ss.getKey().equals("key1")).findFirst().orElseThrow();
        Assert.assertEquals("value with priority", s.getValue());

        settingsWithHighestPriority.stream().filter(ss -> ss.getKey().equals("key2")).findFirst().orElseThrow();
        settingsWithHighestPriority.stream().filter(ss -> ss.getKey().equals("key3")).findFirst().orElseThrow();
    }

    @Test
    public void testGetSettingWithHighestPriority(){
        ProjectSettingAccessorImpl accessor = new ProjectSettingAccessorImpl(new InMemoryCollection<>());
        prepareTestData(accessor);
        ProjectSetting s = accessor.getSettingWithHighestPriority("key1", null);
        Assert.assertEquals("value with priority", s.getValue());
    }

    private static void prepareTestData(ProjectSettingAccessorImpl accessor) {
        ProjectSetting s1 = new ProjectSetting("key1", "value1", "description");
        s1.addAttribute(EntityWithUniqueAttributes.ATTRIBUTE_PRIORITY, "10");

        ProjectSetting s2 = new ProjectSetting("key2", "value2", "description");
        s2.addAttribute(EntityWithUniqueAttributes.ATTRIBUTE_PRIORITY, "10");

        ProjectSetting s3 = new ProjectSetting("key1", "value with priority", "description");
        s3.addAttribute(EntityWithUniqueAttributes.ATTRIBUTE_PRIORITY, "100");

        // without priority attribute
        ProjectSetting s4 = new ProjectSetting("key3", "value3", "description");

        accessor.save(s1);
        accessor.save(s2);
        accessor.save(s3);
        accessor.save(s4);
    }
}