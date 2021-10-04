package step.functions.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.exense.commons.io.FileHelper;
import step.functions.handler.OsCheck.OSType;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;

public class FunctionDependenciesInstaller {

	private static final Logger logger = LoggerFactory.getLogger(FunctionDependenciesInstaller.class);

	private final FileManagerClient fileManagerClient;

	private final ConcurrentHashMap<FileVersionId, Boolean> installedDependenciesTracker = new ConcurrentHashMap<FileVersionId, Boolean>();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final File installedDependenciesTrackingFile;

	public FunctionDependenciesInstaller(FileManagerClient fileManagerClient, File installedDependenciesTrackingFile)
			throws DependencyInstallationException {
		super();
		this.fileManagerClient = fileManagerClient;
		this.installedDependenciesTrackingFile = installedDependenciesTrackingFile;

		readTrackingFile();
	}

	private void readTrackingFile() throws DependencyInstallationException {
		if (installedDependenciesTrackingFile.exists() && installedDependenciesTrackingFile.length() > 0) {
			logger.info(
					"Reading dependency installer tracker file " + installedDependenciesTrackingFile.getAbsolutePath());
			try {
				objectMapper.readValue(installedDependenciesTrackingFile, new TypeReference<List<FileVersionId>>() {
				}).forEach(fileVersionId -> {
					installedDependenciesTracker.put(fileVersionId, Boolean.TRUE);
				});
			} catch (IOException e) {
				throw new DependencyInstallationException("Error while reading dependency tracking file "
						+ installedDependenciesTrackingFile.getAbsolutePath(), e);
			}
		} else {
			logger.info("Creating tracker file for dependency installer under: "
					+ installedDependenciesTrackingFile.getAbsolutePath());
			try {
				installedDependenciesTrackingFile.createNewFile();
			} catch (IOException e) {
				throw new DependencyInstallationException("Error while creating dependency tracking file "
						+ installedDependenciesTrackingFile.getAbsolutePath(), e);
			}
		}
	}

	public File installFunctionDependencies(FileVersionId functionDependenciesPackage)
			throws DependencyInstallationException {
		if (functionDependenciesPackage != null) {
			FileVersion requestFileVersion;
			try {
				requestFileVersion = fileManagerClient.requestFileVersion(functionDependenciesPackage);
			} catch (FileManagerException e) {
				throw new DependencyInstallationException("Error while requesting dependencies from grid", e);
			}

			// Check if this dependency has already been installed
			if (installedDependenciesTracker.putIfAbsent(functionDependenciesPackage, Boolean.TRUE) == null) {
				updateTrackingFile();

				File functionDependencies = requestFileVersion.getFile();
				logger.info("Installing dependency package " + functionDependencies.getAbsolutePath());
				File depFolder;
				try {
					depFolder = FileHelper.createTempFolder();
				} catch (IOException e) {
					throw new DependencyInstallationException("Error while creating temporary folder", e);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Extracting dependency package " + functionDependencies.getAbsolutePath() + " to "
							+ depFolder.getAbsolutePath());
				}
				try {
					FileHelper.unzip(functionDependencies, depFolder);
				} catch (IOException e) {
					throw new DependencyInstallationException(
							"Error while unzipping dependencies to " + depFolder.getAbsolutePath(), e);
				}

				File executionFolder = depFolder.toPath().resolve(functionDependencies.getName().replace(".zip", ""))
						.toFile();

				OSType OSType = OsCheck.getOperatingSystemType();

				List<String> commands = new ArrayList<>();

				String executionFolderPath = executionFolder.getAbsolutePath();
				switch (OSType) {
				case Linux:
					commands.add("sh");
					commands.add(executionFolderPath + "./install.sh");
					break;
				case MacOS:
					commands.add("sh");
					commands.add(executionFolderPath + "./install.sh");
					break;
				case Windows:
					commands.add(executionFolderPath + "/install.bat");
					break;
				default:
					throw new RuntimeException("Unsupported platform");
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Executing dependency package installation script " + commands.toString());
				}

				try {
					Process process = new ProcessBuilder().inheritIO().directory(executionFolder).command(commands)
							.start();
					try {
						process.waitFor(1, TimeUnit.MINUTES);
					} finally {
						process.destroy();
					}
				} catch (IOException | InterruptedException e) {
					throw new DependencyInstallationException(
							"Error while executing dependency installation script: " + e.getMessage(), e);
				}

				return executionFolder;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private synchronized void updateTrackingFile() throws DependencyInstallationException {
		List<FileVersionId> fileVersionIdList = installedDependenciesTracker.keySet().stream()
				.collect(Collectors.toList());
		try {
			objectMapper.writeValue(installedDependenciesTrackingFile, fileVersionIdList);
		} catch (IOException e) {
			throw new DependencyInstallationException("Error while reading dependency tracking file "
					+ installedDependenciesTrackingFile.getAbsolutePath(), e);
		}
	}
}
