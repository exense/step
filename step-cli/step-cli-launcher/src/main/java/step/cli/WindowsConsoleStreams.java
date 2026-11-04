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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Makes the standard output and error of the CLI print every character on Windows, whatever the code page of the
 * console, the way Go programs do:
 * <ul>
 *     <li>to a console, as cmd, PowerShell or Windows Terminal, the text is written as UTF-16 with
 *     {@code WriteConsoleW}, which the code page does not apply to</li>
 *     <li>to anything else, as the terminal of git bash, a pipe or a file, the text is written as UTF-8</li>
 * </ul>
 * Without them, Java encodes the output in the code page of the console, cp850 for instance, which has no character
 * for the markers of the live report, nor for most of the characters of the other languages.
 * <p>
 * It also finds out whether the standard output is a terminal printing colors: a console, once asked to process the
 * ANSI escape sequences, or the terminal of git bash, which Windows only knows as a pipe named after it.
 */
final class WindowsConsoleStreams {

    private static final Logger logger = LoggerFactory.getLogger(WindowsConsoleStreams.class);

    private static final int STD_OUTPUT_HANDLE = -11;
    private static final int STD_ERROR_HANDLE = -12;
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;
    private static final int FILE_NAME_INFO = 2;
    // The pipes of the terminals of git bash (MSYS2) and Cygwin, e.g. \msys-dd50a72ab4668b33-pty0-to-master
    static final Pattern TERMINAL_PIPE_NAME = Pattern.compile(
        "^\\\\(Device\\\\NamedPipe\\\\)?(msys|cygwin)-[0-9a-f]+-pty\\d+-(from|to)-master$");

    private static volatile boolean stdoutColorTerminal;

    private WindowsConsoleStreams() {
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    /**
     * @return if {@link #install} found the standard output to be a terminal printing the ANSI escape sequences as
     * colors
     */
    static boolean isStdoutColorTerminal() {
        return stdoutColorTerminal;
    }

    /**
     * Replaces {@link System#out} and {@link System#err} on Windows, and leaves them as they are elsewhere or if the
     * functions of Windows cannot be called
     */
    static void install() {
        if (!isWindows()) {
            return;
        }
        Kernel32 kernel32;
        try {
            kernel32 = Native.load("kernel32", Kernel32.class);
        } catch (Throwable e) {
            // Typically the native library of JNA not being allowed to be extracted to the temporary folder
            logger.debug("The console streams of Windows cannot be used, the output keeps the encoding of the console", e);
            return;
        }
        Pointer stdout = kernel32.GetStdHandle(STD_OUTPUT_HANDLE);
        stdoutColorTerminal = isConsole(kernel32, stdout) ? enableColors(kernel32, stdout) : isTerminalPipe(kernel32, stdout);
        System.setOut(open(kernel32, stdout, FileDescriptor.out));
        System.setErr(open(kernel32, kernel32.GetStdHandle(STD_ERROR_HANDLE), FileDescriptor.err));
    }

    private static PrintStream open(Kernel32 kernel32, Pointer handle, FileDescriptor fileDescriptor) {
        OutputStream out = isConsole(kernel32, handle) ? new ConsoleOutputStream(kernel32, handle) : new FileOutputStream(fileDescriptor);
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private static boolean isConsole(Kernel32 kernel32, Pointer handle) {
        // Only a console has a mode
        return handle != null && kernel32.GetConsoleMode(handle, new IntByReference());
    }

    /**
     * Asks the console to process the ANSI escape sequences, which the consoles of Windows 10 and later can, until the
     * CLI exits, for the console to be given back to the shell as it was
     */
    private static boolean enableColors(Kernel32 kernel32, Pointer console) {
        IntByReference mode = new IntByReference();
        if (!kernel32.GetConsoleMode(console, mode)) {
            return false;
        }
        int originalMode = mode.getValue();
        if ((originalMode & ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
            return true;
        }
        if (!kernel32.SetConsoleMode(console, originalMode | ENABLE_VIRTUAL_TERMINAL_PROCESSING)) {
            return false;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> kernel32.SetConsoleMode(console, originalMode),
            "console-mode-restorer"));
        return true;
    }

    private static boolean isTerminalPipe(Kernel32 kernel32, Pointer handle) {
        if (handle == null) {
            return false;
        }
        // FILE_NAME_INFO: the length of the name in bytes, followed by the name in UTF-16
        Memory info = new Memory(4 + 2048);
        if (!kernel32.GetFileInformationByHandleEx(handle, FILE_NAME_INFO, info, (int) info.size())) {
            return false;
        }
        int nameLength = Math.min(info.getInt(0) / 2, 1024);
        String name = new String(info.getCharArray(4, nameLength));
        return TERMINAL_PIPE_NAME.matcher(name).matches();
    }

    /**
     * Writes the UTF-8 bytes it is given to a console as UTF-16 characters
     */
    static class ConsoleOutputStream extends OutputStream {

        // Keeps each call within the limits of the consoles of the older versions of Windows
        private static final int MAX_CHARS_PER_WRITE = 8192;

        private final Kernel32 kernel32;
        private final Pointer handle;
        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        // The bytes of a character split across two writes, kept until the next one
        private byte[] pending = new byte[0];

        ConsoleOutputStream(Kernel32 kernel32, Pointer handle) {
            this.kernel32 = kernel32;
            this.handle = handle;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            byte[] bytes = Arrays.copyOf(pending, pending.length + len);
            System.arraycopy(b, off, bytes, pending.length, len);
            ByteBuffer in = ByteBuffer.wrap(bytes);
            // A UTF-8 character never takes fewer bytes than it takes UTF-16 characters
            CharBuffer chars = CharBuffer.allocate(bytes.length);
            decoder.decode(in, chars, false);
            pending = Arrays.copyOfRange(bytes, in.position(), bytes.length);
            writeToConsole(chars.array(), chars.position());
        }

        private void writeToConsole(char[] chars, int length) throws IOException {
            IntByReference written = new IntByReference();
            for (int offset = 0; offset < length; offset += written.getValue()) {
                int end = Math.min(length, offset + MAX_CHARS_PER_WRITE);
                // Without splitting the two halves of a character outside of the basic plane, such as an emoji
                if (end < length && Character.isHighSurrogate(chars[end - 1])) {
                    end--;
                }
                char[] chunk = Arrays.copyOfRange(chars, offset, end);
                if (!kernel32.WriteConsoleW(handle, chunk, chunk.length, written, null) || written.getValue() <= 0) {
                    throw new IOException("Unable to write to the console, error " + Native.getLastError());
                }
            }
        }
    }

    /**
     * The functions of Windows used to write to its consoles
     */
    interface Kernel32 extends StdCallLibrary {

        Pointer GetStdHandle(int nStdHandle);

        boolean GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);

        boolean SetConsoleMode(Pointer hConsoleHandle, int dwMode);

        boolean GetFileInformationByHandleEx(Pointer hFile, int fileInformationClass, Pointer lpFileInformation,
                                             int dwBufferSize);

        boolean WriteConsoleW(Pointer hConsoleOutput, char[] lpBuffer, int nNumberOfCharsToWrite,
                              IntByReference lpNumberOfCharsWritten, Pointer lpReserved);
    }
}
