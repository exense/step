package step.functions.packages;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.io.FileWatchService;

public class FunctionPackageChangeWatcher implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackageChangeWatcher.class);
	
	private FileWatchService fileWatchService;
	private FunctionPackageAccessor accessor;
	private FunctionPackageManager packageManager;
	
	public FunctionPackageChangeWatcher(FunctionPackageAccessor accessor, FunctionPackageManager packageManager, int interval) {
		super();
		this.accessor = accessor;
		this.packageManager = packageManager;
		this.fileWatchService = new FileWatchService();
		this.fileWatchService.setInterval(interval);
	}

	public void registerWatchers() {
		Iterator<FunctionPackage> it = accessor.getAll();
		while(it.hasNext()) {
			FunctionPackage functionPackage=it.next();
			registerWatcherForPackage(functionPackage);
		}
	}

	public void registerWatcherForPackage(FunctionPackage functionPackage) {
		if(functionPackage.isWatchForChange()) {
			File packageFile = getWatchedFile(functionPackage);	
			
			fileWatchService.register(packageFile, ()->{
				String packageId = functionPackage.getId().toString();
				try {
					packageManager.reloadFunctionPackage(packageId);
				} catch (Exception e) {
					logger.error("Error while reloading function package "+packageId+" based on file " + packageFile.getAbsolutePath() , e);
				}
			}, false);
		}
	}

	private File getWatchedFile(FunctionPackage functionPackage) {
		File packageFile = new File(functionPackage.getPackageLocation());
		return packageFile;
	}
	
	public void unregisterWatcher(FunctionPackage functionPackage) {
		File packageFile = getWatchedFile(functionPackage);	
		fileWatchService.unregister(packageFile);
	}

	@Override
	public void close() throws IOException {
		fileWatchService.close();
	}
}
