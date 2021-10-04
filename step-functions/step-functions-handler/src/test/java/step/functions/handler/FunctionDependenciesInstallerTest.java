package step.functions.handler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;

public class FunctionDependenciesInstallerTest {

	@Test
	public void test() throws DependencyInstallationException, IOException {
		File installedDependenciesTrackingFile = new File("dependenciesTracking.json");
		installedDependenciesTrackingFile.delete();
		FunctionDependenciesInstaller isntaller = newFunctionDependenciesInstaller(installedDependenciesTrackingFile);
		FileVersionId fileVersionId = new FileVersionId();
		File executionFolder = isntaller.installFunctionDependencies(fileVersionId);
		// Assert that the installation script has been executed
		File testFile = executionFolder.toPath().resolve("TestFile.txt").toFile();
		assertTrue(testFile.exists());

		assertTrue(installedDependenciesTrackingFile.exists());

		testFile.delete();
		// Reinstall the dependencies
		isntaller.installFunctionDependencies(fileVersionId);
		// It shouldn't get executed again
		assertFalse(testFile.exists());

		// Reload the installer
		isntaller = newFunctionDependenciesInstaller(installedDependenciesTrackingFile);
		// Reinstall the dependencies
		isntaller.installFunctionDependencies(fileVersionId);
		// It shouldn't get executed again
		assertFalse(testFile.exists());
	}

	private FunctionDependenciesInstaller newFunctionDependenciesInstaller(File installedDependenciesTrackingFile)
			throws DependencyInstallationException {
		return new FunctionDependenciesInstaller(new FileManagerClient() {

			@Override
			public FileVersion requestFileVersion(FileVersionId fileVersionId) throws FileManagerException {
				File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(),
						"DependenciesInstall.zip");
				return new FileVersion(file, fileVersionId, false);
			}

			@Override
			public void removeFileVersionFromCache(FileVersionId fileVersionId) {
			}
		}, installedDependenciesTrackingFile);
	}

}
