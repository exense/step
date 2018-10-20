package step.grid.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

import step.commons.helpers.FileHelper;
import step.grid.filemanager.FileProvider.TransportableFile;

public class FIleManagerTest {

	@Test
	public void test() throws FileProviderException, IOException {
		FileManagerServer server = new FileManagerServer();
		
		byte[] content = new byte[]{11};
		
		File testFile = new File("./testFileManager");
		testFile.deleteOnExit();
		Files.write(content, testFile);
		
		String id = server.registerFile(testFile);

		final TransportableFile a = server.getTransportableFile(id);
		Assert.assertNotNull(a);
		Assert.assertArrayEquals(content, a.bytes); 

		AtomicInteger remoteCallCounts = new AtomicInteger(0);
		
		File tempDataFolder = new File("./tempDataFolder");
		tempDataFolder.mkdirs();
		
		try {
			FileManagerClient client = new FileManagerClientImpl(tempDataFolder, new StreamingFileProvider() {
				@Override
				public File saveFileTo(String fileHandle, File file) throws FileProviderException {
					remoteCallCounts.incrementAndGet();
					return testFile;
				}
			});
			File clientFile = client.requestFile(id, 1);
			clientFile = client.requestFile(id, 1);
			Assert.assertEquals(1, remoteCallCounts.get());
			clientFile = client.requestFile(id, 2);
			Assert.assertEquals(2, remoteCallCounts.get());
			clientFile = client.requestFile(id, 2);
			Assert.assertEquals(2, remoteCallCounts.get());
			
			byte[] bytes = Files.toByteArray(clientFile);
			Assert.assertArrayEquals(content, bytes); 
			
			clientFile.delete();
		} finally {
			FileHelper.deleteFolder(tempDataFolder);
		}
	}
	
	@Test
	public void testFileDeletion() throws IOException {
		FileManagerServer server = new FileManagerServer();
		
		byte[] content = new byte[]{11};
		
		File testFile = new File("./testFileManagerFileDeletion");
		testFile.deleteOnExit();
		Files.write(content, testFile);
		
		String id = server.registerFile(testFile);
		Assert.assertNotNull(id);
		
		testFile.delete();
		Exception ex = null;
		try {
			server.registerFile(testFile);
		} catch(Exception e) {
			ex = e;
		}
		Assert.assertNotNull(ex);
		Assert.assertTrue(ex.getMessage().contains("Unable to find or read file"));
		
	}
	
	@Test
	public void testParallel() throws IOException, InterruptedException {
		AtomicInteger remoteCallCounts = new AtomicInteger(0);
		
		File tempDataFolder = new File("./tempDataFolder");
		tempDataFolder.mkdirs();
		try {
			
			final FileManagerClient client = new FileManagerClientImpl(tempDataFolder, new StreamingFileProvider() {
				@Override
				public File saveFileTo(String fileHandle, File file) throws FileProviderException {
					File testFile = new File(file+"/test");
					remoteCallCounts.incrementAndGet();
					return testFile;
				}
			});
			
			ExecutorService e = Executors.newFixedThreadPool(5);
			for(int i=0;i<1000;i++) {
				e.submit(()->{
					try {
						File file = client.requestFile("id", 1);
						file.delete();
					} catch(Exception e1) {
						e1.printStackTrace();
					};});
			}
			
			e.shutdown();
			e.awaitTermination(10, TimeUnit.SECONDS);
			Assert.assertEquals(1, remoteCallCounts.get());
		} finally {
			FileHelper.deleteFolder(tempDataFolder);
		}
		
	}
}
