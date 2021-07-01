package step.functions.packages.handlers;

import java.io.File;
import java.io.FileNotFoundException;

import step.attachments.FileResolver;

public class FunctionPackageUtils {
	protected static final String READY_STRING = "READY";
	
	protected final FileResolver fileResolver;

	public FunctionPackageUtils(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
	}
	
	protected File resolveMandatoryFile(String resource) throws FileNotFoundException {
		File file = resolveFile(resource);
		if (file == null) {
			throw new FileNotFoundException("The resource " + resource + " doesn't exist");
		} else {
			if (!file.exists()) {
				throw new FileNotFoundException("The file " + file + " doesn't exist");
			}
		}
		return file;
	}

	protected File resolveFile(String resource) {
		File file = null;
		if (resource != null) {
			file = fileResolver.resolve(resource);
		}
		return file;
	}

	public static class DiscovererParameters {
		public String packageLibrariesLocation;
		public String packageLocation;

		public DiscovererParameters() {
		}

		public String getPackageLibrariesLocation() {
			return packageLibrariesLocation;
		}

		public void setPackageLibrariesLocation(String packageLibrariesLocation) {
			this.packageLibrariesLocation = packageLibrariesLocation;
		}

		public String getPackageLocation() {
			return packageLocation;
		}

		public void setPackageLocation(String packageLocation) {
			this.packageLocation = packageLocation;
		}
	}
}
