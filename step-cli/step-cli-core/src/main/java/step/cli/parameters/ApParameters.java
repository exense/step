package step.cli.parameters;

import step.cli.StepCliExecutionException;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

public abstract class ApParameters<T extends ApParameters<T>> extends Parameters<T> {

    private File automationPackageFile;
    private MavenArtifactIdentifier automationPackageMavenArtifact;
    private MavenArtifactIdentifier libraryMavenArtifact;
    private File libraryFile;
    private String managedLibraryName;

    /**
     * @return the maven snippet of automation package to be uploaded or null to use the local file instead.
     */
    public MavenArtifactIdentifier getAutomationPackageMavenArtifact() {
        return automationPackageMavenArtifact;
    }

    public T setAutomationPackageMavenArtifact(MavenArtifactIdentifier automationPackageMavenArtifact) {
        this.automationPackageMavenArtifact = automationPackageMavenArtifact;
        //noinspection unchecked
        return (T) this;
    }

    public MavenArtifactIdentifier getLibraryMavenArtifact() {
        return libraryMavenArtifact;
    }

    public T setlibraryMavenArtifact(MavenArtifactIdentifier libraryMavenArtifact) {
        this.libraryMavenArtifact = libraryMavenArtifact;
        //noinspection unchecked
        return (T) this;
    }

    public File getLibraryFile() {
        return libraryFile;
    }

    public T setLibraryFile(File libraryFile) {
        this.libraryFile = libraryFile;
        //noinspection unchecked
        return (T) this;
    }

    public String getManagedLibraryName() {
        return managedLibraryName;
    }

    public T setManagedLibraryName(String managedLibraryName) {
        this.managedLibraryName = managedLibraryName;
        //noinspection unchecked
        return (T) this;
    }

    public File getAutomationPackageFile() {
        return automationPackageFile;
    }

    public T setAutomationPackageFile(File automationPackageFile) {
        this.automationPackageFile = automationPackageFile;
        //noinspection unchecked
        return (T) this;
    }

    public void validate() throws StepCliExecutionException {
        if (getAutomationPackageFile() != null && getAutomationPackageMavenArtifact() != null) {
            throw new StepCliExecutionException("Invalid parameters detected. The automation package should be referenced either as local file or as maven snipped");
        }
        if (getLibraryFile() != null && getLibraryMavenArtifact() != null) {
            throw new StepCliExecutionException("Invalid parameters detected. The automation package library should be referenced either as local file or as maven snipped");
        }
    }
}
