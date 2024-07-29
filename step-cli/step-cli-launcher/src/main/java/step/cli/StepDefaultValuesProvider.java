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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StepDefaultValuesProvider implements CommandLine.IDefaultValueProvider {

    public static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/stepcli.properties";

    private static final Logger log = LoggerFactory.getLogger(StepDefaultValuesProvider.class);

    private CommandLine.PropertiesDefaultProvider delegate;
    private Properties mergedProperties;

    public StepDefaultValuesProvider() {
        this(new ArrayList<>());
    }

    public StepDefaultValuesProvider(List<String> customConfigFiles) {
        try {
            applyCustomConfigFiles(customConfigFiles == null ? new ArrayList<>() : customConfigFiles);
        } catch (IOException ex) {
            throw new RuntimeException("Invalid configuration detected. " + ex.getMessage(), ex);
        }
    }

    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {
        if (delegate != null) {
            return delegate.defaultValue(argSpec);
        } else {
            return argSpec.defaultValue();
        }
    }

    protected void applyCustomConfigFiles(List<String> customConfigFiles) throws IOException {
        // for some parameters (like boolean flags) the value for customConfigFiles is for some reason not passed
        // https://github.com/remkop/picocli/issues/2326
        // so for each argSpec we need to make a recheck to be sure that we don't miss the value of used --config option

        // default value defined in field
        String defaultConfigFile = DEFAULT_CONFIG_FILE;

        String infoText = "Applying";
        infoText += " properties from ";
        if (!customConfigFiles.isEmpty()) {
            infoText += customConfigFiles + " and ";
        }
        infoText += "default config in " + defaultConfigFile;
        log.info(infoText);
        this.mergedProperties = mergeProperties(customConfigFiles, defaultConfigFile);

        this.delegate = new CommandLine.PropertiesDefaultProvider(mergedProperties);
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
                Properties tempProperties = new Properties();
                tempProperties.load(fis);
                for (Map.Entry<Object, Object> temp : tempProperties.entrySet()) {
                    // for "execution parameters" we should merge values but not replace them
                    if (StepConsole.ApCommand.ApExecuteCommand.EP_DESCRIPTION_KEY.equals(temp.getKey())) {
                        res.merge(temp.getKey(), temp.getValue(), (object, object2) -> {
                            if (object2 == null || object.toString().isEmpty()) {
                                return object;
                            }
                            if (object.toString().isEmpty()) {
                                return object2;
                            }
                            return object + "|" + object2;
                        });
                    } else {
                        res.put(temp.getKey(), temp.getValue());
                    }
                }
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
