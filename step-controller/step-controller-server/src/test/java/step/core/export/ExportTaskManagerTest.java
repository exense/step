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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import ch.exense.commons.io.Poller;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.resources.Resource;


public class ExportTaskManagerTest {
	
	@Test
	public void test() throws InterruptedException, TimeoutException {
		ExportTaskManager m = new ExportTaskManager(null);
		AtomicInteger i = new AtomicInteger(0);
		final ExportStatus s = m.createExportTask(new ExportRunnable() {

			@Override
			protected Resource runExport() throws Exception {
				i.incrementAndGet();
				Resource resource = new Resource();
				return resource;
			}
			
		});
		
		Poller.waitFor(()->m.getExportStatus(s.getId()).ready, 2000);
		Assert.assertEquals(0, m.exportStatusMap.size());
		Assert.assertEquals(1, i.get());
	}
	
	@Test
	public void testException() throws InterruptedException, TimeoutException {
		ExportTaskManager m = new ExportTaskManager(null);
		final ExportStatus s = m.createExportTask(new ExportRunnable() {

			@Override
			protected Resource runExport() throws Exception {
				throw new RuntimeException();
			}
			
		});
		Poller.waitFor(()->m.getExportStatus(s.getId()).ready, 2000);
		Assert.assertEquals(0, m.exportStatusMap.size());
	}

}
