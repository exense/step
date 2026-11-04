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

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.junit.Assert;
import org.junit.Test;
import step.cli.WindowsConsoleStreams.ConsoleOutputStream;
import step.cli.WindowsConsoleStreams.Kernel32;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WindowsConsoleStreamsTest {

    @Test
    public void writesTheCharactersToTheConsole() {
        Console console = new Console();
        PrintStream out = new PrintStream(new ConsoleOutputStream(console, null), true, StandardCharsets.UTF_8);

        out.println("✔ Commande passée > Échec : 😀");

        Assert.assertEquals("✔ Commande passée > Échec : 😀" + System.lineSeparator(), console.text());
    }

    /**
     * Logback encodes the events itself and writes their bytes, which a character might be split across
     */
    @Test
    public void writesACharacterSplitAcrossTwoWrites() throws IOException {
        Console console = new Console();
        ConsoleOutputStream out = new ConsoleOutputStream(console, null);
        byte[] bytes = "a✔b".getBytes(StandardCharsets.UTF_8);

        out.write(bytes, 0, 2);
        out.write(bytes, 2, bytes.length - 2);

        Assert.assertEquals("a✔b", console.text());
    }

    @Test
    public void writesALongTextInSeveralCalls() throws IOException {
        Console console = new Console();
        String text = "x".repeat(20000);

        new ConsoleOutputStream(console, null).write(text.getBytes(StandardCharsets.UTF_8));

        Assert.assertEquals(text, console.text());
        Assert.assertEquals(List.of(8192, 8192, 3616), console.callLengths);
    }

    /**
     * The terminal of git bash is a pipe for Windows, named after the terminal
     */
    @Test
    public void recognizesThePipesOfTheTerminalsOfGitBashAndCygwin() {
        for (String name : List.of("\\msys-dd50a72ab4668b33-pty0-to-master", "\\cygwin-e022582115c10879-pty3-from-master",
            "\\Device\\NamedPipe\\msys-dd50a72ab4668b33-pty1-to-master")) {
            Assert.assertTrue(name, WindowsConsoleStreams.TERMINAL_PIPE_NAME.matcher(name).matches());
        }
        for (String name : List.of("\\msys-dd50a72ab4668b33-pty0-to-slave", "\\my-pipe", "\\msys-xyz-pty0-to-master")) {
            Assert.assertFalse(name, WindowsConsoleStreams.TERMINAL_PIPE_NAME.matcher(name).matches());
        }
    }

    private static class Console implements Kernel32 {

        private final StringBuilder text = new StringBuilder();
        private final List<Integer> callLengths = new ArrayList<>();

        @Override
        public Pointer GetStdHandle(int nStdHandle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean SetConsoleMode(Pointer hConsoleHandle, int dwMode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean GetFileInformationByHandleEx(Pointer hFile, int fileInformationClass, Pointer lpFileInformation,
                                                    int dwBufferSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean WriteConsoleW(Pointer hConsoleOutput, char[] lpBuffer, int nNumberOfCharsToWrite,
                                     IntByReference lpNumberOfCharsWritten, Pointer lpReserved) {
            text.append(lpBuffer, 0, nNumberOfCharsToWrite);
            callLengths.add(nNumberOfCharsToWrite);
            lpNumberOfCharsWritten.setValue(nNumberOfCharsToWrite);
            return true;
        }

        private String text() {
            return text.toString();
        }
    }
}
