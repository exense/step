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

import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionNotice;
import step.core.execution.model.ExecutionNoticeSeverity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves persisted {@link ExecutionNotice} instances into {@link ResolvedExecutionNotice}s by
 * looking up their type in the {@link ExecutionNoticeRegistry} and rendering the type's message
 * template against the instance parameters.
 * <p>
 * Resolution rules:
 * <ul>
 *   <li>the message template is trusted HTML and is emitted as-is;</li>
 *   <li>every parameter value substituted into a {@code {placeholder}} is HTML-escaped, so
 *       user-controlled values cannot inject markup;</li>
 *   <li>placeholders without a matching parameter are left untouched;</li>
 *   <li>an unknown notice type never fails resolution: a neutral {@code INFO} fallback is produced.</li>
 * </ul>
 */
public class ExecutionNoticeResolver {

    /** Matches {@code {placeholderName}} tokens, where the name is a Java-style identifier. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_.-]+)}");

    static final String UNKNOWN_TYPE_CATEGORY = "unknown";

    private final ExecutionNoticeRegistry registry;

    public ExecutionNoticeResolver(ExecutionNoticeRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    /**
     * Resolves all notices of the given execution. Returns an empty list when the execution is null
     * or carries no notice.
     */
    public List<ResolvedExecutionNotice> resolve(Execution execution) {
        if (execution == null || execution.getNotices() == null) {
            return Collections.emptyList();
        }
        return execution.getNotices().stream().map(this::resolve).collect(Collectors.toList());
    }

    /**
     * Resolves a single notice instance.
     */
    public ResolvedExecutionNotice resolve(ExecutionNotice notice) {
        ExecutionNoticeType type = (notice.getTypeId() != null) ? registry.get(notice.getTypeId()) : null;
        if (type == null) {
            return resolveUnknown(notice);
        }
        String message = renderMessage(type.getMessageTemplate(), notice.getParameters());
        return new ResolvedExecutionNotice(notice.getTypeId(), type.getCategory(), type.getSeverity(), message, notice.getTimestamp());
    }

    private ResolvedExecutionNotice resolveUnknown(ExecutionNotice notice) {
        Map<String, String> parameters = notice.getParameters();
        String params = (parameters != null) ? parameters.toString() : "";
        String message = "Unknown execution notice type '" + escapeHtml(notice.getTypeId()) + "'"
            + (params.isEmpty() ? "" : " (parameters: " + escapeHtml(params) + ")");
        return new ResolvedExecutionNotice(notice.getTypeId(), UNKNOWN_TYPE_CATEGORY, ExecutionNoticeSeverity.INFO, message, notice.getTimestamp());
    }

    private String renderMessage(String template, Map<String, String> parameters) {
        if (template == null) {
            return "";
        }
        Map<String, String> params = (parameters != null) ? parameters : Collections.emptyMap();
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement;
            if (params.containsKey(key)) {
                // Parameter values are HTML-escaped; the trusted template provides any intended markup.
                replacement = escapeHtml(params.get(key));
            } else {
                // Leave unresolved placeholders untouched so a missing parameter is visible rather than silently dropped.
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
