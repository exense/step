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
package step.plugins.table.settings;

import step.plugins.screentemplating.ScreenInput;

public class ScreenInputColumnSettings extends ColumnSettings {
    private ScreenInput screenInput;

    public ScreenInputColumnSettings() {
    }

    public ScreenInputColumnSettings(String columnId, boolean visible, int position, ScreenInput screenInput) {
        super(columnId, visible, position);
        this.screenInput = screenInput;
    }

    public ScreenInput getScreenInput() {
        return screenInput;
    }

    public void setScreenInput(ScreenInput screenInput) {
        this.screenInput = screenInput;
    }
}
