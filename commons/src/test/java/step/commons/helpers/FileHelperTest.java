package step.commons.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class FileHelperTest {

	@Test
	public void testDeleteOnExitReucrsive() throws IOException {
		File dir = new File("./FileHelperTestFolder/");
		dir.mkdir();
		
		File f1 = new File(dir.getAbsolutePath()+"/f1");
		f1.createNewFile();
		
		File f2 = new File(dir.getAbsolutePath()+"/f2");
		f2.createNewFile();
		
		File subDir1 = new File(dir.getAbsolutePath()+"/subFolder1");
		subDir1.mkdir();
		
		FileHelper.deleteFolderOnExit(dir);
	}
	
	@Test
	public void testComputeLastModificationDateRecursive() throws IOException {
		File dir = new File("FileHelperTestFolder/");
		dir.mkdir();
		dir.deleteOnExit();
		
		File f1 = new File(dir.getAbsolutePath()+"/f1");
		f1.deleteOnExit();
		f1.createNewFile();
		
		File f2 = new File(dir.getAbsolutePath()+"/f1");
		f2.deleteOnExit();
		f2.createNewFile();
		
		File subDir1 = new File(dir.getAbsolutePath()+"/subFolder1");
		subDir1.deleteOnExit();
		subDir1.mkdir();
		
		File f4 = new File(subDir1.getAbsolutePath()+"/f1");
		f4.deleteOnExit();
		f4.createNewFile();
		
		dir.setLastModified(10000);
		subDir1.setLastModified(10000);
		f1.setLastModified(20000);
		f2.setLastModified(30000);
		f4.setLastModified(20000);
		
		long lastModif = FileHelper.computeLastModificationDateRecursive(dir);
		Assert.assertEquals(30000, lastModif);
		
		f4.setLastModified(40000);
		lastModif = FileHelper.computeLastModificationDateRecursive(dir);
		Assert.assertEquals(40000, lastModif);
		
		subDir1.setLastModified(50000);
		lastModif = FileHelper.computeLastModificationDateRecursive(dir);
		Assert.assertEquals(50000, lastModif);
		
	}
	
	@Test
	public void testGetLastModificationDateFromCache() throws IOException, InterruptedException {
		File dir = new File("FileHelperTestFolder2/");
		dir.mkdir();
		dir.deleteOnExit();
		
		File f1 = new File(dir.getAbsolutePath()+"/f1");
		f1.deleteOnExit();
		f1.createNewFile();
		
		dir.setLastModified(10000);
		f1.setLastModified(20000);
		callInParallel(dir,20000);

		dir.setLastModified(30000);
		Thread.sleep(2000);
		callInParallel(dir,30000);
	}

	private void callInParallel(File dir, long lastmodif) throws InterruptedException {
		final AtomicBoolean exception = new AtomicBoolean(false);

		ExecutorService service = Executors.newFixedThreadPool(10);
		for(int i=0;i<10;i++) {
			service.submit(new Runnable() {
				@Override
				public void run() {
					for(int j=0;j<1000;j++) {
						try {
							long lastModif = FileHelper.getLastModificationDateRecursive(dir);
							Assert.assertEquals(lastmodif, lastModif);						
						} catch (Throwable e) {
							e.printStackTrace();
							exception.set(true);
						}						
					}
				}
			});
		}
		
		service.shutdown();
		service.awaitTermination(10, TimeUnit.SECONDS);
		Assert.assertFalse(exception.get());
	}
	
	@Test
	public void testUnzip() throws IOException {
		byte[] bytes = Files.readAllBytes(new File(getClass().getResource("/testUnzip.zip").getFile()).toPath());
		
		File target = new File("TestUnzip" + UUID.randomUUID().toString());
		try {
			FileHelper.extractFolder(bytes, target);
			
			Assert.assertTrue(new File(target.getAbsoluteFile()+"/testUnzip/file1.txt").exists());
			Assert.assertTrue(new File(target.getAbsoluteFile()+"/testUnzip/subFolder/file2.txt").exists());
		} finally {
			FileHelper.deleteFolderOnExit(target);
		}

	}
}
