package step.engine.plugins;

import ch.exense.commons.io.FileHelper;
import step.grid.filemanager.FileManagerException;
import step.plugins.java.handler.KeywordHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LocalFunctionHandler extends KeywordHandler {

    @Override
    public File retrieveAndExtractAutomationPackageFile(Map<String, String> properties) throws FileManagerException {
        try {
            // For local Function we artificially create an archive using the application classloader defined as system property
            File tempFile = FileHelper.createTempFile();
            ClassLoaderArchiver.createArchive(tempFile, true);
            return extractAutomationPackageFile(tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary archive file for the classloader", e);
        }
    }

    @Override
    public boolean containsAutomationPackageFileReference(Map<String, String> properties) {
        //for local Keyword we always consider that we hare in the context of an AP
        return true;
    }}
