package step.core.export;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.commons.helpers.Poller;
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
