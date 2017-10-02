package step.core.export;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.attachments.AttachmentManager;
import step.commons.conf.Configuration;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;


public class ExportTaskManagerTest {
	
	@Test
	public void test() throws InterruptedException {
		AttachmentManager am = new AttachmentManager(new Configuration());
		ExportTaskManager m = new ExportTaskManager(new AttachmentManager(new Configuration()));
		AtomicInteger i = new AtomicInteger(0);
		ExportStatus s = m.createExportTask(new ExportRunnable() {

			@Override
			protected void runExport() throws Exception {
				i.incrementAndGet();
				File f = new File(getContainer()+"/test");
				f.createNewFile();
			}
			
		});
		Thread.sleep(100);
		Assert.assertEquals(1, i.get());
		Assert.assertEquals(1, m.exportStatusMap.size());
				
		s = m.getExportStatus(s.getId());
		Assert.assertTrue(s.ready);
		Assert.assertEquals(0, m.exportStatusMap.size());
		
		am.deleteContainer(s.getAttachmentID());
	}
	
	@Test
	public void testException() throws InterruptedException {
		AttachmentManager am = new AttachmentManager(new Configuration());
		ExportTaskManager m = new ExportTaskManager(new AttachmentManager(new Configuration()));
		ExportStatus s = m.createExportTask(new ExportRunnable() {

			@Override
			protected void runExport() throws Exception {
				throw new RuntimeException();
			}
			
		});
		Thread.sleep(100);
				
		s = m.getExportStatus(s.getId());
		Assert.assertTrue(s.ready);
		Assert.assertEquals(0, m.exportStatusMap.size());
		
		am.deleteContainer(s.getAttachmentID());
	}

}
