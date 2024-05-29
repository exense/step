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
package step.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static step.cli.Parameters.AP_FILE;

public class ApDeployCliHandler implements CliCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ApDeployCliHandler.class);

    @Override
    public Integer execute(CliConfig config) {
        File apFile;
        try {
            apFile = config.getFile(AP_FILE, true);
            String stepUrl = config.getString(Parameters.STEP_URL, true);
            String artifactGroupId = config.getString(Parameters.ARTIFACT_GROUP_ID, false);
            String artifactId = config.getString(Parameters.ARTIFACT_ID, false);
            String artifactVersion = config.getString(Parameters.ARTIFACT_VERSION, false);
            String artifactClassifier = config.getString(Parameters.ARTIFACT_CLASSIFIER, false);
            String stepProjectName = config.getString(Parameters.PROJECT_NAME, false);
            String token = config.getString(Parameters.TOKEN, false);
            Boolean async = config.getBoolean(Parameters.ASYNC, false);
            runTool(stepUrl, artifactGroupId, artifactId, artifactVersion, artifactClassifier, stepProjectName, token, async, apFile);
            return 0;
        } catch (StepCliExecutionException ex) {
            log.error(ex.getMessage());
            return -1;
        }
    }

    protected void runTool(String stepUrl, String artifactGroupId, String artifactId, String artifactVersion, String artifactClassifier, String stepProjectName, String token, Boolean async, File apFile) {
        new AbstractDeployAutomationPackageTool(stepUrl, artifactGroupId, artifactId, artifactVersion, artifactClassifier, stepProjectName, token, async) {
            @Override
            protected File getFileToUpload() throws StepCliExecutionException {
                return apFile;
            }

            @Override
            protected void logError(String errorText, Throwable e) {
                if (e != null) {
                    log.error(errorText, e);
                } else {
                    log.error(errorText);
                }
            }

            @Override
            protected void logInfo(String infoText, Throwable e) {
                if (e != null) {
                    log.info(infoText, e);
                } else {
                    log.info(infoText);
                }
            }
        }.execute();
    }
}
