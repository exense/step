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
package step.commons.helpers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import step.commons.conf.Configuration;

public class FileHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(FileHelper.class);

	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
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
	
	public static void deleteFolderOnExit(File folder) {
		folder.deleteOnExit();
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolderOnExit(f);
				} else {
					f.deleteOnExit();
				}
			}
		}
	}
	
	static LoadingCache<File, Long> cache = CacheBuilder.newBuilder()
				.concurrencyLevel(Configuration.getInstance().getPropertyAsInteger("filehelper.cache.concurrencylevel", 4))
				.maximumSize(Configuration.getInstance().getPropertyAsInteger("filehelper.cache.maximumsize", 1000))
				.expireAfterWrite(Configuration.getInstance().getPropertyAsInteger("filehelper.cache.expireafter.seconds", 1), TimeUnit.SECONDS)
				.build(new CacheLoader<File, Long>() {
					public Long load(File file) {
						return computeLastModification(file);
					}
				});
	
	public static final long getLastModificationDateRecursive(File file) {
		try {
			return cache.get(file);
		} catch (ExecutionException e) {
			throw new RuntimeException("Error while getting last modification date for file '"+file.getAbsolutePath()+"' from cache",e);
		}
	}
	
	protected static final long computeLastModification(File file) {
		return computeLastModificationDateRecursive(file);
	}
	
	protected static final long computeLastModificationDateRecursive(File file) {
		if(file.isDirectory()) {
			long lastModificationDate = file.lastModified();
			for(File f:file.listFiles()) {
				long lastChange = computeLastModificationDateRecursive(f);
				if(lastChange>lastModificationDate) {
					lastModificationDate = lastChange;
				}
			}
			return lastModificationDate;
		} else {
			return file.lastModified();
		}
	}

	public static final void zipDirectory(File directory, File zip) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
		zip(directory, directory, zos);
		zos.close();
	}
	
	
	public static void extractFolder(byte[] bytes, File target) 
	{
		ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
	    try
	    {
	        int BUFFER = 2048;

	        target.mkdir();

	        ZipEntry entry;
	        // Process each entry
	        while ((entry=zip.getNextEntry())!=null)
	        {
	            String currentEntry = entry.getName();

	            File destFile = new File(target.getAbsolutePath(), currentEntry);
	            File destinationParent = destFile.getParentFile();

	            destinationParent.mkdirs();

	            if (!entry.isDirectory())
	            {

	                int currentByte;
	                // establish buffer for writing file
	                byte data[] = new byte[BUFFER];

	                // write the current file to disk
	                FileOutputStream fos = new FileOutputStream(destFile);
	                BufferedOutputStream dest = new BufferedOutputStream(fos,
	                BUFFER);

	                // read and write until last byte is encountered
	                while ((currentByte = zip.read(data, 0, BUFFER)) != -1) {
	                    dest.write(data, 0, currentByte);
	                }
	                dest.flush();
	                dest.close();
	            }


	        }
	    }
	    catch (Exception e) 
	    {
	    	logger.error("Error while extracting folder "+target.getAbsolutePath(), e);
	    }
	    finally {
	    	try {
				zip.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

	}
	
	public static final byte[] zipDirectory(File directory) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(out);
		zip(directory, directory, zos);
		zos.close();
		return out.toByteArray();
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
	
	public static File getClassLoaderResource(Class<?> clazz, String resourceName) {
		try {
			URL url = clazz.getClassLoader().getResource(resourceName);
			// workaround: doing toURI().getPath() to decode %20 in case of spaces in path
			return url!=null?new File(url.toURI().getPath()):null;
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error while parsing URI of resource "+resourceName,e);
		}
	}
	
	public static String readStream(InputStream is){
		try(Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
			return scanner.useDelimiter("\\A").next().replaceAll("\r\n", "\n");
		}
	}
	
	public static String readResource(Class<?> clazz, String resourceName){
		try(Scanner scanner = new Scanner(clazz.getResourceAsStream(resourceName), StandardCharsets.UTF_8.name())) {
			return scanner.useDelimiter("\\A").next().replaceAll("\r\n", "\n");
		}
	}

}
