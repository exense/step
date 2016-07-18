package FileWatchService;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import step.commons.conf.FileWatchService;

public class FileWatchServiceTest {

	@Test
	public void testBasic() {
		File file = new File(this.getClass().getClassLoader().getResource("FileWatchServiceTest.test").getFile());
		final AtomicInteger updatedCount = new AtomicInteger(0);
		FileWatchService.getInstance().setInterval(1000);
		FileWatchService.getInstance().register(file, new Runnable() {
			@Override
			public void run() {
				updatedCount.incrementAndGet();
			}
		});
		for(int i=0;i<2;i++) {
			file.setLastModified(System.currentTimeMillis());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Assert.assertEquals(2,updatedCount.get());
		FileWatchService.getInstance().unregister(file);
		file.setLastModified(System.currentTimeMillis());
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals(2,updatedCount.get());
		//FileWatchService.getInstance().interrupt();
	}
}
