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
package step.plugins.java;

import step.automation.packages.AutomationPackage;
import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.AutomationPackageContextual;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityConstants;
import step.core.entities.EntityReference;
import step.functions.Function;

/**
 * This class encapsulates the configuration parameters of functions (aka Keywords)
 * of type "Script"
 *
 */
public class GeneralScriptFunction extends Function implements AutomationPackageContextual<GeneralScriptFunction> {

    public static final String $_MARK_AS_KEYWORD_FROM_AUTOMATION_PACKAGE_LIBRARY = "$markAsKeywordFromAutomationPackageLibrary";

    DynamicValue<String> scriptFile = new DynamicValue<>("");

    DynamicValue<String> scriptLanguage = new DynamicValue<>("");

    DynamicValue<String> librariesFile = new DynamicValue<>("");

    DynamicValue<String> errorHandlerFile = new DynamicValue<>("");

    @EntityReference(type = EntityConstants.resources)
    public DynamicValue<String> getScriptFile() {
        return scriptFile;
    }

    /**
     * @param scriptFile the path to the script file (.js, .groovy, .jar, depending on the script language)
     */
    public void setScriptFile(DynamicValue<String> scriptFile) {
        this.scriptFile = scriptFile;
    }

    public DynamicValue<String> getScriptLanguage() {
        return scriptLanguage;
    }

    /**
     * @param scriptLanguage the script language of this function. Per default the following language are supported: javascript, groovy, java
     */
    public void setScriptLanguage(DynamicValue<String> scriptLanguage) {
        this.scriptLanguage = scriptLanguage;
    }

    @EntityReference(type = EntityConstants.resources)
    public DynamicValue<String> getLibrariesFile() {
        return librariesFile;
    }

    /**
     * @param librariesFile the path to the libraries to be used for the function. This can be a single jar or a folder containing
     *                      a list of jars
     */
    public void setLibrariesFile(DynamicValue<String> librariesFile) {
        this.librariesFile = librariesFile;
    }

    @EntityReference(type = EntityConstants.resources)
    public DynamicValue<String> getErrorHandlerFile() {
        return errorHandlerFile;
    }

    /**
     * @param errorHandlerFile the path to the script to be executed when the function returns an error
     */
    public void setErrorHandlerFile(DynamicValue<String> errorHandlerFile) {
        this.errorHandlerFile = errorHandlerFile;
    }

    @Override
    public GeneralScriptFunction applyAutomationPackageContext(StagingAutomationPackageContext context) {
        //Only process function without script file set (i.e. Keywords from scanned annotations)
        if (getScriptFile().get() == null || getScriptFile().get().isEmpty()) {
            AutomationPackage ap = context.getAutomationPackage();
            if (ap == null) {
                throw new RuntimeException("General script functions defined in Automation Packages must either be declared in the descriptor providing an explicit script file or with Keyword annotation.");
            }
            //Handle Keywords declared in AP library, the library is used as script file for them
            if (getCustomField($_MARK_AS_KEYWORD_FROM_AUTOMATION_PACKAGE_LIBRARY) != null) {
                getCustomFields().remove($_MARK_AS_KEYWORD_FROM_AUTOMATION_PACKAGE_LIBRARY);
                if (ap.getAutomationPackageLibraryResourceRevision() != null && !ap.getAutomationPackageLibraryResourceRevision().isEmpty()) {
                    setScriptFile(new DynamicValue<>(ap.getAutomationPackageLibraryResourceRevision()));
                } else {
                    throw new RuntimeException("Inconsistent state: the annotated Keyword '" + this.getAttribute(NAME) + "' was detected in an Automation Package Library, but the library resource does not exists.");
                }
            } else {
                //Keyword annotated in main AP file
                if (ap.getAutomationPackageResourceRevision() != null && !ap.getAutomationPackageResourceRevision().isEmpty()) {
                    setScriptFile(new DynamicValue<>(ap.getAutomationPackageResourceRevision()));
                } else {
                    throw new RuntimeException("Inconsistent state: the annotated Keyword '" + this.getAttribute(NAME) + "' was detected in an Automation Package, but the package resource does not exists.");
                }
                if (ap.getAutomationPackageLibraryResourceRevision() != null && !ap.getAutomationPackageLibraryResourceRevision().isEmpty()) {
                    setLibrariesFile(new DynamicValue<>(ap.getAutomationPackageLibraryResourceRevision()));
                }
            }
        }
        return this;
    }
}
