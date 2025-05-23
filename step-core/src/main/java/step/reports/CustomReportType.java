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
package step.reports;

public enum CustomReportType {
    JUNITXML("junit"), // report
    JUNITZIP("junit"); // zipped report with attachments

    private final String nameInFile;

    CustomReportType(String nameInFile) {
        this.nameInFile = nameInFile;
    }

    public static CustomReportType parse(String customReportType) {
        for (CustomReportType value : CustomReportType.values()) {
            if (value.name().equalsIgnoreCase(customReportType)) {
                return value;
            }
        }
        return null;
    }

    public String getNameInFile() {
        return nameInFile;
    }
}
