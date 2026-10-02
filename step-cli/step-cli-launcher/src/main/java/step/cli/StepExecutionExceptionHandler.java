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

public class StepExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(StepExecutionExceptionHandler.class);

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {
        CommandLine.Model.OptionSpec verboseOption = commandLine.getCommandSpec().findOption(StepConsole.AbstractStepCommand.VERBOSE);
        if (verboseOption == null) {
            // commands to launch IDE are not related to executions and do not have the verbose flag, so we just perform "generic" error handling
            StepConsole.log.error("Unhandled exception", ex);
            return CommandLine.ExitCode.SOFTWARE;
        } else {
            // The stack traces are kept out of the output of a non verbose run (see StepConsoleLogging.suppressStackTraces),
            // which is worth saying once, here, where every failing command ends up
            log.error("Execution failed. " + ex.getMessage() + ". Run the command with "
                + StepConsole.AbstractStepCommand.VERBOSE + " to see the stack traces of the errors.");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }
}
