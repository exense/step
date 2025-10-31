package step.cli.parameters;

import step.cli.StepCliExecutionException;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

public abstract class ApParameters<T extends ApParameters<T>> extends Parameters<T> {

    private File automationPackageFile;
    private MavenArtifactIdentifier automationPackageMavenArtifact;
    private MavenArtifactIdentifier automationPackageLibraryMavenArtifact;
    private File automationPackageLibraryFile;
    private String automationPackageManagedLibraryName;

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

    public MavenArtifactIdentifier getAutomationPackageLibraryMavenArtifact() {
        return automationPackageLibraryMavenArtifact;
    }

    public T setPackageLibraryMavenArtifact(MavenArtifactIdentifier packageLibraryMavenArtifact) {
        this.automationPackageLibraryMavenArtifact = packageLibraryMavenArtifact;
        //noinspection unchecked
        return (T) this;
    }

    public File getAutomationPackageLibraryFile() {
        return automationPackageLibraryFile;
    }

    public T setPackageLibraryFile(File packageLibraryFile) {
        this.automationPackageLibraryFile = packageLibraryFile;
        //noinspection unchecked
        return (T) this;
    }

    public String getAutomationPackageManagedLibraryName() {
        return automationPackageManagedLibraryName;
    }

    public T setAutomationPackageManagedLibraryName(String automationPackageManagedLibraryName) {
        this.automationPackageManagedLibraryName = automationPackageManagedLibraryName;
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
        if (getAutomationPackageLibraryFile() != null && getAutomationPackageLibraryMavenArtifact() != null) {
            throw new StepCliExecutionException("Invalid parameters detected. The automation package library should be referenced either as local file or as maven snipped");
        }
    }
}
