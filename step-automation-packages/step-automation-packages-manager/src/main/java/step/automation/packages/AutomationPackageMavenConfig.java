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
package step.automation.packages;

import step.core.objectenricher.ObjectFilter;

import java.io.File;

public class AutomationPackageMavenConfig {

    private String mavenSettingsXml;
    private File localFileRepository;

    public AutomationPackageMavenConfig(String mavenSettingsXml, File localFileRepository) {
        this.mavenSettingsXml = mavenSettingsXml;
        this.localFileRepository = localFileRepository;
    }

    public String getMavenSettingsXml() {
        return mavenSettingsXml;
    }

    public void setMavenSettingsXml(String mavenSettingsXml) {
        this.mavenSettingsXml = mavenSettingsXml;
    }

    public File getLocalFileRepository() {
        return localFileRepository;
    }

    public void setLocalFileRepository(File localFileRepository) {
        this.localFileRepository = localFileRepository;
    }

    public interface ConfigProvider {
        AutomationPackageMavenConfig getConfig(ObjectFilter objectFilter);
    }
}
