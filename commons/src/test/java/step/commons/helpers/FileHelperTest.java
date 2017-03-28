package step.commons.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class FileHelperTest {

	@Test
	public void testGetLastModificationDateRecursive() throws IOException {
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
		
		dir.setLastModified(1);
		subDir1.setLastModified(1);
		f1.setLastModified(2);
		f2.setLastModified(3);
		f4.setLastModified(2);

		long lastModif = FileHelper.getLastModificationDateRecursive(dir);
		Assert.assertEquals(3, lastModif);
		
		f4.setLastModified(4);
		lastModif = FileHelper.getLastModificationDateRecursive(dir);
		Assert.assertEquals(4, lastModif);
		
		subDir1.setLastModified(5);
		lastModif = FileHelper.getLastModificationDateRecursive(dir);
		Assert.assertEquals(5, lastModif);
		
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
			FileHelper.deleteFolder(target);
		}

	}
}
