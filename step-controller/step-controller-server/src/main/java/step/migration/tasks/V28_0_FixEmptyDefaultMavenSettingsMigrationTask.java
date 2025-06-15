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
package step.migration.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.controller.ControllerSetting;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.Objects;

public class V28_0_FixEmptyDefaultMavenSettingsMigrationTask extends MigrationTask {

    protected static final String OLD_MAVEN_EMPTY_SETTINGS =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                    "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                    "</settings>\n";

    // The actual value in ArtifactRepositoryConstants
    public static final String NEW_MAVEN_EMPTY_SETTINGS =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                    "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                    "    <activeProfiles>\n" +
                    "        <activeProfile>default</activeProfile>\n" +
                    "    </activeProfiles>\n" +
                    "\n" +
                    "    <profiles>\n" +
                    "        <profile>\n" +
                    "            <id>default</id>\n" +
                    "            <repositories>\n" +
                    "                <repository>\n" +
                    "                    <id>central</id>\n" +
                    "                    <url>https://repo1.maven.org/maven2/</url>\n" +
                    "                </repository>\n" +
                    "            </repositories>\n" +
                    "        </profile>\n" +
                    "    </profiles>\n" +
                    "</settings>";

    private static final Logger log = LoggerFactory.getLogger(V28_0_FixEmptyDefaultMavenSettingsMigrationTask.class);

    public V28_0_FixEmptyDefaultMavenSettingsMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3,28,0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        log.info("Migrating default maven settings");
        XmlMapper xmlMapper = new XmlMapper();

        Collection<ControllerSetting> settingsCollection = collectionFactory.getCollection("settings", ControllerSetting.class);
        ControllerSetting setting = settingsCollection.find(Filters.equals("key", "maven_settings_default"), null, null, null, 0).findFirst().orElse(null);
        if(setting != null && setting.getValue() != null){
            log.info("Existing default maven settings detected");
            try {
                JsonNode xmlInDb = xmlMapper.readTree(setting.getValue());
                JsonNode oldEmptyMavenSettings = xmlMapper.readTree(OLD_MAVEN_EMPTY_SETTINGS);

                // compare xmls as json nodes to avoid issues with whitespaces and line separators
                if (Objects.equals(xmlInDb, oldEmptyMavenSettings)) {
                    log.info("Empty default maven settings detected. Settings will be migrated");
                    setting.setValue(NEW_MAVEN_EMPTY_SETTINGS);
                    settingsCollection.save(setting);
                    log.info("Default maven settings have been successfully migrated");
                } else {
                    log.info("Default maven setting is not empty. Migration is not required");
                }
            } catch (JsonProcessingException e) {
                log.error("Invalid xml detected in maven_settings_default. Migration failed", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void runDowngradeScript() {

    }
}
