package step.commons.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileHelper {

	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	public static final void zipDirectory(File directory, File zip) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
		zip(directory, directory, zos);
		zos.close();
	}

	private static final void zip(File directory, File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[8192];
		int read = 0;
		for (int i = 0, n = files.length; i < n; i++) {
			if (files[i].isDirectory()) {
				zip(files[i], base, zos);
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	public static void zip(String sourceDir, String zipFile) {
		try {
			// create object of FileOutputStream
			FileOutputStream fout = new FileOutputStream(zipFile);
			// create object of ZipOutputStream from FileOutputStream
			ZipOutputStream zout = new ZipOutputStream(fout);
			// create File object from source directory
			File fileSource = new File(sourceDir);
			addDirectory(zout, fileSource);

			// close the ZipOutputStream
			zout.close();
		} catch (IOException ioe) {

			System.out.println("IOException :" + ioe);

		}

	}

	private static void addDirectory(ZipOutputStream zout, File fileSource) {

		// get sub-folder/files list
		File[] files = fileSource.listFiles();

		System.out.println("Adding directory " + fileSource.getName());
		for (int i = 0; i < files.length; i++) {
			// if the file is directory, call the function recursively
			if (files[i].isDirectory()) {
				addDirectory(zout, files[i]);
				continue;
			}

			/*
			 * 
			 * 
			 * we are here means, its file and not directory, so
			 * 
			 * 
			 * add it to the zip file
			 */

			try {
				System.out.println("Adding file " + files[i].getName());
				// create byte buffer
				byte[] buffer = new byte[1024];
				// create object of FileInputStream
				FileInputStream fin = new FileInputStream(files[i]);
				zout.putNextEntry(new ZipEntry(files[i].getName()));

				/*
				 * 
				 * 
				 * After creating entry in the zip file, actually
				 * 
				 * 
				 * write the file.
				 */

				int length;
				while ((length = fin.read(buffer)) > 0) {
					zout.write(buffer, 0, length);
				}

				/*
				 * 
				 * 
				 * After writing the file to ZipOutputStream, use
				 * 
				 * 
				 * 
				 * 
				 * 
				 * void closeEntry() method of ZipOutputStream class to
				 * 
				 * 
				 * close the current entry and position the stream to
				 * 
				 * 
				 * write the next entry.
				 */

				zout.closeEntry();
				// close the InputStream
				fin.close();
			} catch (IOException ioe) {
				System.out.println("IOException :" + ioe);
			}

		}

	}

}
