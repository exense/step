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
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionNotice;

import java.util.List;
import java.util.Map;

public class ExecutionNoticeManagerTest {

    private ExecutionNotice notice(int i) {
        return new ExecutionNotice("t", Map.of("i", String.valueOf(i)));
    }

    @Test
    public void appendsUntilCapThenAddsSentinelOnce() {
        ExecutionNoticeManager manager = new ExecutionNoticeManager(3);
        Execution execution = new Execution();

        for (int i = 0; i < 10; i++) {
            manager.appendWithCap(execution, notice(i));
        }

        List<ExecutionNotice> notices = execution.getNotices();
        // 3 real notices + exactly 1 sentinel
        Assert.assertEquals(4, notices.size());
        Assert.assertEquals("t", notices.get(0).getTypeId());
        Assert.assertEquals("t", notices.get(2).getTypeId());
        Assert.assertEquals(ExecutionNoticeManager.NOTICES_SUPPRESSED_TYPE_ID, notices.get(3).getTypeId());
        Assert.assertEquals("3", notices.get(3).getParameters().get("limit"));
    }

    @Test
    public void capDisabledAppendsEverything() {
        ExecutionNoticeManager manager = new ExecutionNoticeManager(0);
        Execution execution = new Execution();
        for (int i = 0; i < 250; i++) {
            manager.appendWithCap(execution, notice(i));
        }
        Assert.assertEquals(250, execution.getNotices().size());
    }

    @Test
    public void suppressedSentinelTypeIsRegisteredAndResolvable() {
        ExecutionNoticeManager manager = new ExecutionNoticeManager(1);
        Execution execution = new Execution();
        manager.appendWithCap(execution, notice(0));
        manager.appendWithCap(execution, notice(1)); // triggers sentinel

        List<ResolvedExecutionNotice> resolved = manager.resolve(execution);
        // first notice resolves via unknown fallback (type "t" not registered here), sentinel resolves via built-in type
        ResolvedExecutionNotice sentinel = resolved.get(resolved.size() - 1);
        Assert.assertEquals(ExecutionNoticeManager.NOTICES_SUPPRESSED_TYPE_ID, sentinel.getTypeId());
        Assert.assertTrue(sentinel.getMessage().contains("limit of 1"));
    }
}
