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

import org.junit.Assert;
import org.junit.Test;
import step.core.execution.model.ExecutionNotice;
import step.core.execution.model.ExecutionNoticeSeverity;

import java.util.Map;

public class ExecutionNoticeResolverTest {

    private ExecutionNoticeResolver resolverWith(ExecutionNoticeType type) {
        ExecutionNoticeRegistry registry = new ExecutionNoticeRegistry();
        registry.register(type);
        return new ExecutionNoticeResolver(registry);
    }

    @Test
    public void substitutesPlaceholdersAndKeepsTrustedTemplateMarkup() {
        ExecutionNoticeResolver resolver = resolverWith(new ExecutionNoticeType(
            "t", "cardinality", ExecutionNoticeSeverity.WARNING,
            "High cardinality on <b>{labelName}</b> of metric <b>{metricName}</b> (quota {quota})."));

        ResolvedExecutionNotice resolved = resolver.resolve(new ExecutionNotice("t",
            Map.of("labelName", "user_id", "metricName", "m1", "quota", "20")));

        Assert.assertEquals("cardinality", resolved.getCategory());
        Assert.assertEquals(ExecutionNoticeSeverity.WARNING, resolved.getSeverity());
        Assert.assertEquals("High cardinality on <b>user_id</b> of metric <b>m1</b> (quota 20).", resolved.getMessage());
    }

    @Test
    public void parameterValuesAreHtmlEscapedButTemplateIsNot() {
        ExecutionNoticeResolver resolver = resolverWith(new ExecutionNoticeType(
            "t", "cat", ExecutionNoticeSeverity.WARNING, "label <b>{labelName}</b>"));

        ResolvedExecutionNotice resolved = resolver.resolve(new ExecutionNotice("t",
            Map.of("labelName", "<script>alert('x')</script>")));

        // The trusted <b> markup survives; the user-controlled value is escaped.
        Assert.assertEquals("label <b>&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;</b>", resolved.getMessage());
    }

    @Test
    public void unresolvedPlaceholderIsLeftUntouched() {
        ExecutionNoticeResolver resolver = resolverWith(new ExecutionNoticeType(
            "t", "cat", ExecutionNoticeSeverity.INFO, "a {present} b {absent}"));

        ResolvedExecutionNotice resolved = resolver.resolve(new ExecutionNotice("t", Map.of("present", "X")));
        Assert.assertEquals("a X b {absent}", resolved.getMessage());
    }

    @Test
    public void unknownTypeFallsBackToInfoAndNeverThrows() {
        ExecutionNoticeResolver resolver = new ExecutionNoticeResolver(new ExecutionNoticeRegistry());

        ResolvedExecutionNotice resolved = resolver.resolve(new ExecutionNotice("does.not.exist", Map.of("k", "v")));
        Assert.assertEquals(ExecutionNoticeResolver.UNKNOWN_TYPE_CATEGORY, resolved.getCategory());
        Assert.assertEquals(ExecutionNoticeSeverity.INFO, resolved.getSeverity());
        Assert.assertEquals("Unknown execution notice type 'does.not.exist' (parameters: {k=v})", resolved.getMessage());
    }
}
