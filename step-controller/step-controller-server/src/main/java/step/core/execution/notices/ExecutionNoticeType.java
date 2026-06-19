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
 * Definition of an execution notice. Notice types are registered by plugins at startup in the
 * {@link ExecutionNoticeRegistry} and are referenced by their {@link #id} from the lightweight
 * {@code ExecutionNotice} instances persisted on executions.
 * <p>
 * The {@link #messageTemplate} is <b>trusted HTML</b> authored by the plugin developer. It may
 * contain {@code {placeholder}} tokens (resolved at read time from the notice instance parameters)
 * and static HTML such as {@code <a href="...">} links. Parameter values substituted into the
 * template are HTML-escaped at resolution time; the template itself is not.
 */
public class ExecutionNoticeType {

    private final String id;
    private final String category;
    private final ExecutionNoticeSeverity severity;
    private final String messageTemplate;

    /**
     * @param id              the unique, namespaced id of this notice type (e.g. {@code "timeseries.label-cardinality-quota-exceeded"})
     * @param category        a free-form grouping key (e.g. {@code "cardinality"}), used to group/filter notices in the UI
     * @param severity        the presentation severity; never affects the execution result or status
     * @param messageTemplate trusted HTML message template with optional {@code {placeholder}} tokens
     */
    public ExecutionNoticeType(String id, String category, ExecutionNoticeSeverity severity, String messageTemplate) {
        this.id = id;
        this.category = category;
        this.severity = severity;
        this.messageTemplate = messageTemplate;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public ExecutionNoticeSeverity getSeverity() {
        return severity;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }
}
