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
import step.core.execution.model.ExecutionNoticeSeverity;

import java.util.List;

public class ExecutionNoticeRegistryTest {

    private ExecutionNoticeType type(String id) {
        return new ExecutionNoticeType(id, "cat", ExecutionNoticeSeverity.WARNING, "msg {x}");
    }

    @Test
    public void registerGetAndListPreserveOrder() {
        ExecutionNoticeRegistry registry = new ExecutionNoticeRegistry();
        registry.register(type("a"));
        registry.register(type("b"));

        Assert.assertNotNull(registry.get("a"));
        Assert.assertEquals("b", registry.get("b").getId());
        Assert.assertNull(registry.get("missing"));

        List<ExecutionNoticeType> all = registry.getAll();
        Assert.assertEquals(List.of("a", "b"), all.stream().map(ExecutionNoticeType::getId).collect(java.util.stream.Collectors.toList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateRegistrationFailsFast() {
        ExecutionNoticeRegistry registry = new ExecutionNoticeRegistry();
        registry.register(type("dup"));
        registry.register(type("dup"));
    }
}
