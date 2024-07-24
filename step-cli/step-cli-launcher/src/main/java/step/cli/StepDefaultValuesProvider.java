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
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StepDefaultValuesProvider implements CommandLine.IDefaultValueProvider {

    public static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/stepcli.properties";

    private static final Logger log = LoggerFactory.getLogger(StepDefaultValuesProvider.class);

    private boolean configOptionApplied = false;
    private CommandLine.PropertiesDefaultProvider delegate;
    private Properties mergedProperties;

    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {
        if (!configOptionApplied) {
            CommandLine.Model.OptionSpec customConfigFile = argSpec.command().findOption(StepConsole.AbstractStepCommand.CONFIG);

            if (customConfigFile != null) {
                configOptionApplied = true;

                List<String> customConfigFiles = customConfigFile.originalStringValues();

                // default value defined in annotation
                String defaultConfigFile = customConfigFile.defaultValue();

                if (defaultConfigFile == null) {
                    // superdefault - in user home (if there is no default value in annotation)
                    defaultConfigFile = DEFAULT_CONFIG_FILE;
                }

                String infoText = "Applying";
                infoText += " properties from " + customConfigFiles + " and";
                infoText += " default config from " + defaultConfigFile;
                log.info(infoText) ;
                this.mergedProperties = mergeProperties(customConfigFiles, defaultConfigFile);

                this.delegate = new CommandLine.PropertiesDefaultProvider(mergedProperties);
            }
        }
        if (delegate != null) {
            return delegate.defaultValue(argSpec);
        } else {
            return argSpec.defaultValue();
        }
    }

    private Properties mergeProperties(List<String> customConfigFiles, String defaultConfigFile) throws IOException {
        Properties res = new Properties();

        if (defaultConfigFile != null) {
            addFileToProperties(defaultConfigFile, res);
        }

        for (String pathToFile : customConfigFiles) {
            boolean fileExists = addFileToProperties(pathToFile, res);
            if (!fileExists) {
                log.error("Invalid config file configured (file doesn't exist): " + pathToFile + ". This config file will be ignored.");
            }
        }

        return res;
    }

    private boolean addFileToProperties(String pathToFile, Properties res) throws IOException {
        File configFile = new File(pathToFile);
        if (configFile.exists() && configFile.canRead()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                res.load(fis);
            }
            return true;
        }
        return false;
    }

    public void printAppliedConfig() {
        if (mergedProperties != null) {
            StringBuilder builder = new StringBuilder("Merged configuration files:");
            for (Map.Entry<Object, Object> entries : mergedProperties.entrySet()) {
                builder.append("\n").append("- ").append(entries.getKey().toString()).append("=").append(entries.getValue().toString());
            }
            log.info(builder.toString());
        }
    }
}
