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

class StepDefaultValuesProvider implements CommandLine.IDefaultValueProvider {

    private static final Logger log = LoggerFactory.getLogger(StepDefaultValuesProvider.class);

    private boolean configOptionApplied = false;
    private CommandLine.PropertiesDefaultProvider delegate;

    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {
        if (!configOptionApplied) {
            CommandLine.Model.OptionSpec customConfigFile = argSpec.command().findOption(Parameters.CONFIG);

            if (customConfigFile != null) {
                configOptionApplied = true;

                boolean isDefaultConfigFile = false;
                String pathToCustomConfig = customConfigFile.originalStringValues().isEmpty() ? null : customConfigFile.originalStringValues().get(0);
                if (pathToCustomConfig == null) {
                    pathToCustomConfig = customConfigFile.defaultValue();
                    isDefaultConfigFile = true;
                }

                if (pathToCustomConfig != null) {
                    log.info("Setup default properties source: " + pathToCustomConfig);
                    File configFile = new File(pathToCustomConfig);
                    if (configFile.exists() && configFile.canRead()) {
                        this.delegate = new CommandLine.PropertiesDefaultProvider(configFile);
                    } else if (!isDefaultConfigFile) {
                        throw new RuntimeException("Invalid custom config file configured: " + pathToCustomConfig);
                    }
                }
            }
        }
        if (delegate != null) {
            return delegate.defaultValue(argSpec);
        } else {
            return argSpec.defaultValue();
        }
    }
}
