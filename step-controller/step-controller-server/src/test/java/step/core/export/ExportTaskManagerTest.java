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
package step.core.export;

import ch.exense.commons.io.Poller;
import org.bson.types.ObjectId;
import org.junit.Test;
import step.resources.Resource;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ExportTaskManagerTest {

    @Test
    public void test() throws InterruptedException, TimeoutException {
        ExportTaskManager m = new ExportTaskManager(null);
        AtomicInteger i = new AtomicInteger(0);
        final ExportStatus s = m.createExportTask(handle -> {
            i.incrementAndGet();
            assertNotNull(handle.getResourceManager());
            return new Resource();
        });

        Poller.waitFor(() -> m.getExportStatus(s.getId()).isReady(), 2000);
        assertEquals(0, m.getCurrentExportStatus().size());
        assertEquals(1, i.get());
    }

    @Test
    public void testException() throws InterruptedException, TimeoutException {
        ExportTaskManager m = new ExportTaskManager(null);
        final ExportStatus s = m.createExportTask(handle -> {
            throw new RuntimeException();
        });
        Poller.waitFor(() -> m.getExportStatus(s.getId()).isReady(), 2000);
        assertEquals(0, m.getCurrentExportStatus().size());
    }

    @Test
    public void testWarnings() throws InterruptedException, TimeoutException {
        ObjectId resourceId = new ObjectId();
        AtomicBoolean done = new AtomicBoolean();
        ExportTaskManager m = new ExportTaskManager(null);
        final ExportStatus s = m.createExportTask(handle -> {
            handle.setWarnings(Set.of("Warning 1"));
            Resource resource = new Resource();
            resource.setId(resourceId);
            done.set(true);
            return resource;
        });
        Poller.waitFor(done::get, 2000);
        ExportStatus exportStatus = m.getExportStatus(s.getId());
        assertEquals(resourceId.toString(), exportStatus.getAttachmentID());
        assertEquals(Set.of("Warning 1"), exportStatus.getWarnings());
    }

}
