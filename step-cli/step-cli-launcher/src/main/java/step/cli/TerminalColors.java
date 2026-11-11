/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells whether the standard output of the CLI is a terminal printing the ANSI escape sequences as colors, rather than
 * a file or another program, which would get the sequences as text. The colors are disabled by setting the
 * environment variable {@code NO_COLOR} (see <a href="https://no-color.org">no-color.org</a>).
 */
final class TerminalColors {

    private static final Logger logger = LoggerFactory.getLogger(TerminalColors.class);

    private static final int STDOUT_FILE_DESCRIPTOR = 1;

    private static Boolean supportedOnStdout;

    private TerminalColors() {
    }

    static synchronized boolean isSupportedOnStdout() {
        if (supportedOnStdout == null) {
            supportedOnStdout = detect();
        }
        return supportedOnStdout;
    }

    private static boolean detect() {
        String noColor = System.getenv("NO_COLOR");
        if ((noColor != null && !noColor.isEmpty()) || "dumb".equals(System.getenv("TERM"))) {
            return false;
        }
        if (WindowsConsoleStreams.isWindows()) {
            // Found out as the streams are installed, the consoles of Windows only printing colors once asked to
            return WindowsConsoleStreams.isStdoutColorTerminal();
        }
        try {
            return Native.load("c", CLibrary.class).isatty(STDOUT_FILE_DESCRIPTOR) == 1;
        } catch (Throwable e) {
            logger.debug("Unable to find out whether the standard output is a terminal, printing without colors", e);
            return false;
        }
    }

    interface CLibrary extends Library {

        int isatty(int fileDescriptor);
    }
}
