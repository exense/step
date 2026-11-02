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

/**
 * How the report of a local execution is printed out
 */
public enum LocalReportMode {
    /**
     * The report tree of each plan, once the plan has been executed
     */
    tree(false, true),
    /**
     * Each report node as it starts and ends, see {@link LiveReportTreePrinterPlugin}
     */
    live(true, false),
    /**
     * Both of the above
     */
    both(true, true);

    private final boolean printsLive;
    private final boolean printsTree;

    LocalReportMode(boolean printsLive, boolean printsTree) {
        this.printsLive = printsLive;
        this.printsTree = printsTree;
    }

    public boolean printsLive() {
        return printsLive;
    }

    public boolean printsTree() {
        return printsTree;
    }
}
