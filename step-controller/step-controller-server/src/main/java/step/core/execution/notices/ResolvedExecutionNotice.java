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
package step.core.execution.notices;

import step.core.execution.model.ExecutionNoticeSeverity;

/**
 * Read-time, fully resolved view of an execution notice: the message has been rendered from the
 * notice type's template and the instance parameters (with parameter values HTML-escaped). This is
 * the shape returned by the REST endpoints; it is never persisted.
 */
public class ResolvedExecutionNotice {

    private String typeId;
    private String category;
    private ExecutionNoticeSeverity severity;
    private String message;
    private long timestamp;

    public ResolvedExecutionNotice() {
        super();
    }

    public ResolvedExecutionNotice(String typeId, String category, ExecutionNoticeSeverity severity, String message, long timestamp) {
        this.typeId = typeId;
        this.category = category;
        this.severity = severity;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ExecutionNoticeSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ExecutionNoticeSeverity severity) {
        this.severity = severity;
    }

    /**
     * @return the resolved, sanitized HTML message
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
