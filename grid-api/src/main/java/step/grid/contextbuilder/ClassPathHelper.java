package step.grid.contextbuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClassPathHelper {

	public static List<URL> forSingleFile(File file) {
		List<URL> urls = new ArrayList<>();
		try {
			addFileToUrls(urls, file);
		} catch (IOException e) {
			throw new RuntimeException("Error getting url list for file "+file.getAbsolutePath());
		}
		return urls;
	}
	
	public static List<URL> forAllJarsInFolderUsingFilter(File folder, FilenameFilter addtitionalFilter) {
		List<URL> urls = new ArrayList<>();
		
		try {
			addFilesToUrls(urls, folder, new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory()||(pathname.getName().endsWith(".jar")&&(addtitionalFilter==null||addtitionalFilter.accept(folder, pathname.getName())));
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Error getting url list for directory "+folder.getAbsolutePath());
		}
		return urls;
	}
	
	public static List<URL> forAllJarsInFolder(File folder) {
		return forAllJarsInFolderUsingFilter(folder, null);
	}
	
	public static List<URL> forClassPathString(String classPathString) throws MalformedURLException, IOException {
		List<URL> urls = new ArrayList<>();
		
		String[] paths = classPathString.split(";");
		for(String path:paths) {
			File f = new File(path);
			addFileToUrls(urls, f);				
			urls.add(f.getCanonicalFile().toURI().toURL());
		}
		return urls;
	}

	private static void addFilesToUrls(List<URL> urls, File f, FileFilter filter) throws IOException {
		if(f.isDirectory()) {
			for(File file:f.listFiles(filter)) {
				addFilesToUrls(urls, file, filter);
			}
		} else {
			addFileToUrls(urls, f);
		}
	}
	
	private static void addFileToUrls(List<URL> urls, File f) throws IOException {
		urls.add(f.getCanonicalFile().toURI().toURL());
	}
}
