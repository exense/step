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
package step.automation.packages;

import step.core.maven.MavenArtifactIdentifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AutomationPackageFileSource {

    private MavenArtifactIdentifier mavenArtifactIdentifier;

    private String resourceId;
    private String fileName;
    private InputStream inputStream;
    private String managedLibraryName;

    public enum Mode {
        INPUT_STREAM,
        MAVEN,
        RESOURCE_ID,
        MANAGED_LIBRARY_NAME,
        NONE
    }

    private AutomationPackageFileSource(){
    }

    public static AutomationPackageFileSource withMavenIdentifier(MavenArtifactIdentifier mavenArtifactIdentifier){
        AutomationPackageFileSource res = new AutomationPackageFileSource();
        res.mavenArtifactIdentifier = mavenArtifactIdentifier;
        return res;
    }

    public static AutomationPackageFileSource withInputStream(InputStream inputStream, String fileName){
        AutomationPackageFileSource res = new AutomationPackageFileSource();
        res.inputStream = inputStream;
        res.fileName = fileName;
        return res;
    }

    public static AutomationPackageFileSource withResourceId(String resourceId){
        AutomationPackageFileSource res = new AutomationPackageFileSource();
        res.resourceId = resourceId;
        return res;
    }

    public static AutomationPackageFileSource withManagedLibraryName(String managedLibraryName){
        AutomationPackageFileSource res = new AutomationPackageFileSource();
        res.managedLibraryName = managedLibraryName;
        return res;
    }

    public static AutomationPackageFileSource empty(){
        return new AutomationPackageFileSource();
    }

    public MavenArtifactIdentifier getMavenArtifactIdentifier() {
        return mavenArtifactIdentifier;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getManagedLibraryName() {
        return managedLibraryName;
    }

    public void setInputStream(InputStream inputStream, String fileName){
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    public void setMavenIdentifier(MavenArtifactIdentifier mavenArtifactIdentifier){
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
    }


    public void setResourceId(String resourceId){
        this.resourceId = resourceId;
    }

    public void setManagedLibraryKey(String managedLibraryName) {
        this.managedLibraryName = managedLibraryName;
    }

    public Mode getMode() throws AutomationPackageManagerException {
        List<Mode> modes = new ArrayList<>();
        if (getMavenArtifactIdentifier() != null) {
            modes.add(Mode.MAVEN);
        }
        if (getInputStream() != null) {
            modes.add(Mode.INPUT_STREAM);
        }
        if (getResourceId() != null) {
            modes.add(Mode.RESOURCE_ID);
        }
        if (getManagedLibraryName() != null) {
            modes.add(Mode.MANAGED_LIBRARY_NAME);
        }
        if (modes.size() > 1) {
            throw new AutomationPackageManagerException("Ambiguous file definition :" + modes + ". Please use only one of these modes");
        }
        if (modes.isEmpty()) {
            return Mode.NONE;
        } else {
            return modes.get(0);
        }
    }

    @Override
    public String toString() {
        return "AutomationPackageFileSource{" +
                "mavenArtifactIdentifier=" + mavenArtifactIdentifier +
                ", resourceId='" + resourceId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", inputStream=" + (inputStream == null ? "no" : "yes") +
                '}';
    }
}
