/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package FileWatchService;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.commons.conf.FileWatchService;
import step.commons.helpers.FileHelper;

public class FileWatchServiceTest {

	@Test
	public void testBasic() throws InterruptedException {
		File file = FileHelper.getClassLoaderResource(this.getClass(),"FileWatchServiceTest.test");
		
		Object lock = new Object();
		
		final AtomicInteger updatedCount = new AtomicInteger(0);
		FileWatchService.getInstance().setInterval(10);
		FileWatchService.getInstance().register(file, new Runnable() {
			@Override
			public void run() {
				updatedCount.incrementAndGet();
				synchronized (lock) {
					lock.notify();
				}
			}
		});
		
		long lastModified = System.currentTimeMillis();
				
		touchAndWait(file, lock, updatedCount, 1, lastModified);
		touchAndWait(file, lock, updatedCount, 2, lastModified+10000);

		FileWatchService.getInstance().unregister(file);
		touchAndWait(file, lock, updatedCount, 2, lastModified+20000);

	}

	private void touchAndWait(File file, Object lock, final AtomicInteger updatedCount, int expected, long lastModified) throws InterruptedException {
		synchronized (lock) {
			file.setLastModified(lastModified);
			lock.wait(10000);
		}
		Assert.assertEquals(expected,updatedCount.get());
	}
}
