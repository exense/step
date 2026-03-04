package step.engine.plugins;

import ch.exense.commons.classloader.ClassLoaderArchiver;
import ch.exense.commons.io.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.filemanager.FileManagerException;
import step.plugins.java.handler.KeywordHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LocalFunctionHandler extends KeywordHandler {

    protected static Logger logger = LoggerFactory.getLogger(LocalFunctionHandler.class);


    public static String TOKEN_RESERVATION_AUTOMATION_PACKAGE_FILE = "TOKEN_RESERVATION_AUTOMATION_PACKAGE_FILE";

    @Override
    public File retrieveAndExtractAutomationPackageFile(Map<String, String> properties) throws FileManagerException {
        // For local Function we artificially create an archive using the application classloader defined as system property
        TemporaryFile temporaryFile = getTokenReservationSession().getOrDefault(TOKEN_RESERVATION_AUTOMATION_PACKAGE_FILE, u ->
        {
            try {
                return new TemporaryFile(FileHelper.createTempFile());
            } catch (IOException e) {
                throw new RuntimeException("Unable to create temporary archive file", e);
            }
        }, null);
        try {
            ClassLoaderArchiver.createArchive(temporaryFile.temporaryFile, ClassLoaderArchiver.getResourceFilter());
            return extractAutomationPackageFile(temporaryFile.temporaryFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary archive file from the classloader", e);
        }
    }

    private static class TemporaryFile implements AutoCloseable {

        private final File temporaryFile;

        public TemporaryFile(File temporaryFile) {
            this.temporaryFile = temporaryFile;
        }

        @Override
        public void close() {
            if (temporaryFile != null && temporaryFile.exists()) {
                if (!temporaryFile.delete()) {
                    logger.warn("Unable to delete temporary file {}", temporaryFile.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public boolean containsAutomationPackageFileReference(Map<String, String> properties) {
        //for local Keyword we always consider that we are in the context of an AP
        return true;
    }}
