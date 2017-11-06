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

import step.grid.filemanager.FileProvider.TransportableFile;

public class FIleManagerTest {

	@Test
	public void test() throws IOException {
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
		
		FileManagerClient client = new FileManagerClientImpl(new File("."), new FileProvider() {
			@Override
			public TransportableFile getTransportableFile(String fileHandle) throws IOException {
				remoteCallCounts.incrementAndGet();
				return a;
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
	}
	
	@Test
	public void testParallel() throws IOException, InterruptedException {
		AtomicInteger remoteCallCounts = new AtomicInteger(0);
		
		final FileManagerClient client = new FileManagerClientImpl(new File("."), new FileProvider() {
			@Override
			public TransportableFile getTransportableFile(String fileHandle) throws IOException {
				remoteCallCounts.incrementAndGet();
				return new TransportableFile("", false, new byte[]{11});
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
		
	}
}
