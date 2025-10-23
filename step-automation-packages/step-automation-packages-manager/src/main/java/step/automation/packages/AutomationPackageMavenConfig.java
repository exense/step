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

import step.core.objectenricher.ObjectPredicate;

import java.io.File;
import java.time.Duration;

public class AutomationPackageMavenConfig {

    private String mavenSettingsXml;
    private File localFileRepository;
    private Duration maxAge;
    private Duration cleanupFrequency;

    public AutomationPackageMavenConfig(String mavenSettingsXml, File localFileRepository, Duration maxAge, Duration cleanupFrequency) {
        this.mavenSettingsXml = mavenSettingsXml;
        this.localFileRepository = localFileRepository;
        this.maxAge = maxAge;
        this.cleanupFrequency = cleanupFrequency;
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

    public Duration getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }

    public Duration getCleanupFrequency() {
        return cleanupFrequency;
    }

    public void setCleanupFrequency(Duration cleanupFrequency) {
        this.cleanupFrequency = cleanupFrequency;
    }

    public interface ConfigProvider {
        AutomationPackageMavenConfig getConfig(ObjectPredicate objectPredicate);
    }
}
