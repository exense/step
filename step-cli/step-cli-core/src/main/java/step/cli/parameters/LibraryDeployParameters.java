package step.cli.parameters;

import step.cli.StepCliExecutionException;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

public class LibraryDeployParameters extends Parameters<LibraryDeployParameters> {
    private MavenArtifactIdentifier libraryMavenArtifact;
    private File libraryFile;
    private String managedLibraryName;

    public MavenArtifactIdentifier getLibraryMavenArtifact() {
        return libraryMavenArtifact;
    }

    public LibraryDeployParameters setPackageLibraryMavenArtifact(MavenArtifactIdentifier packageLibraryMavenArtifact) {
        this.libraryMavenArtifact = packageLibraryMavenArtifact;
        return this;
    }

    public File getLibraryFile() {
        return libraryFile;
    }

    public LibraryDeployParameters setPackageLibraryFile(File packageLibraryFile) {
        this.libraryFile = packageLibraryFile;
        return this;
    }

    public String getManagedLibraryName() {
        return managedLibraryName;
    }

    public LibraryDeployParameters setManagedLibraryName(String managedLibraryName) {
        this.managedLibraryName = managedLibraryName;
        return this;
    }

    public void validate() {
        if ((getLibraryFile() == null) == (getLibraryMavenArtifact() == null)) {
            throw new StepCliExecutionException("Invalid parameters detected. The automation package library should be referenced either as local file or as maven snipped");
        }
    }
}
