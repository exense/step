package step.grid.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FIleManagerTest {

	@Test
	public void test() throws IOException {
		FileManagerServer server = new FileManagerServer();
		
		byte[] content = new byte[]{11};
		
		File testFile = new File("./testFileManager");
		testFile.deleteOnExit();
		Files.write(content, testFile);
		
		String id = server.registerFile(testFile);

		final Attachment a = server.getFile(id);
		Assert.assertNotNull(a);
		Assert.assertArrayEquals(content, AttachmentHelper.hexStringToByteArray(a.getHexContent())); 

		AtomicInteger remoteCallCounts = new AtomicInteger(0);
		
		FileManagerClient client = new FileManagerClient(new File("."), new FileProvider() {
			@Override
			public Attachment getFile(String fileId) {
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
	}
}
