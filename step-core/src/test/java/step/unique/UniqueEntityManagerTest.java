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

package step.unique;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.collections.Collection;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.objectenricher.ObjectValidator;
import step.projectsettings.ProjectSetting;

public class UniqueEntityManagerTest {
    private static final Logger log = LoggerFactory.getLogger(UniqueEntityManagerTest.class);

    @Test
    public void testObjectValidator(){
        UniqueEntityManager manager = new UniqueEntityManager();

        InMemoryCollectionFactory inMemoryCollectionFactory = new InMemoryCollectionFactory(null);
        Collection<ProjectSetting> projectSettingCollection = inMemoryCollectionFactory.getCollection(ProjectSetting.ENTITY_NAME, ProjectSetting.class);
        ProjectSetting ps1 = new ProjectSetting("setting1", "value1", "description1");
        ps1.addAttribute("name", "setting1");
        ps1.addAttribute("project", "project1");
        projectSettingCollection.save(ps1);

        ProjectSetting ps2 = new ProjectSetting("setting2", "value2", "description2");
        ps1.addAttribute("name", "setting2");
        ps1.addAttribute("project", "project2");
        projectSettingCollection.save(ps2);

        ObjectValidator validator = manager.createObjectValidator(inMemoryCollectionFactory);

        // new entity - ok
        ProjectSetting newPs = new ProjectSetting("setting3", "value3", "description3");
        newPs.addAttribute("name", "setting3");
        newPs.addAttribute("project", "project_new");
        validator.validateOnSave(newPs);

        // update the same entity (with the same id) - ok
        newPs = new ProjectSetting("setting1", "updated value", "updated description");
        newPs.setId(ps1.getId());
        newPs.addAttribute("name", "setting1");
        newPs.addAttribute("project", "project1");
        validator.validateOnSave(newPs);

        // entity with the same key and name, but for another project - ok
        newPs = new ProjectSetting("setting1", "updated value", "updated description");
        newPs.addAttribute("name", "setting1");
        newPs.addAttribute("project", "project_new");
        validator.validateOnSave(newPs);

        // collision - the same key, name and project
        // entity with the same key and name, but for another project - ok
        newPs = new ProjectSetting("setting1", "updated value", "updated description");
        newPs.addAttribute("name", "setting1");
        newPs.addAttribute("project", "project1");

        try {
            validator.validateOnSave(newPs);
            Assert.fail("Exception should be thrown here");
        } catch (Exception e) {
            log.info("Collision detected - OK: {}", e.getMessage());
        }

    }
}