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
package step.core.filebrowser;

import java.text.Collator;
import java.util.Comparator;

public class FileDescriptors {

    private FileDescriptors() {
    }

    /**
     * The listing order every browse service must apply, so that the shared UI component gets the same
     * order whatever it lists: directories first, then names collated locale-sensitively.
     * <p>
     * A new comparator is returned on each call rather than a constant: {@link Collator} is not
     * thread-safe, and building one per listing is cheap.
     */
    public static Comparator<FileDescriptor> byDirectoryThenName() {
        Collator collator = Collator.getInstance();
        // SECONDARY strength ignores case differences (a = A) but respects accents and umlauts (a < ä)
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<FileDescriptor, Boolean>comparing(FileDescriptor::directory).reversed()
            .thenComparing(FileDescriptor::name, collator);
    }
}
