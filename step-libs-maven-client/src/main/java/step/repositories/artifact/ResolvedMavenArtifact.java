package step.repositories.artifact;

import java.io.File;

public class ResolvedMavenArtifact {

    public final SnapshotMetadata snapshotMetadata;
    public final File artifactFile;

    public ResolvedMavenArtifact(File artifactFile, SnapshotMetadata snapshotMetadata) {
        this.snapshotMetadata = snapshotMetadata;
        this.artifactFile = artifactFile;
    }
}
