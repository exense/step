package step.repositories.artifact;

public class SnapshotMetadata {
    public final String mavenTimestamp;
    public final int buildNumber;
    public final long timestamp;
    public final boolean newSnapshotVersion;

    public SnapshotMetadata(String mavenTimestamp, long timestamp, int buildNumber, boolean newSnapshotVersion) {
        this.mavenTimestamp = mavenTimestamp;
        this.buildNumber = buildNumber;
        this.timestamp = timestamp;
        this.newSnapshotVersion = newSnapshotVersion;
    }


}
