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
package step.automation.packages.client;

import step.automation.packages.execution.AutomationPackageExecutionParameters;

import java.io.Closeable;
import java.io.File;
import java.util.List;

public interface AutomationPackageClient extends Closeable {

    String createAutomationPackage(File automationPackageFile) throws AutomationPackageClientException;

    String createOrUpdateAutomationPackage(File automationPackageFile) throws AutomationPackageClientException;

    List<String> executeAutomationPackage(File automationPackageFile, AutomationPackageExecutionParameters params) throws AutomationPackageClientException;

    void deleteAutomationPackage(String packageName) throws AutomationPackageClientException;
}
