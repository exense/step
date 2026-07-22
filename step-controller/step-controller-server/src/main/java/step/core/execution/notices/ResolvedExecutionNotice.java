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
 * the shape returned by the REST endpoints; it is immutable and never persisted.
 *
 * @param message the resolved, sanitized HTML message
 */
public record ResolvedExecutionNotice(
    String typeId,
    String category,
    ExecutionNoticeSeverity severity,
    String message,
    long timestamp
) {
}
