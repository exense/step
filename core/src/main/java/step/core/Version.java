package step.core;

public class Version implements Comparable<Version> {


	int major;
	
	int minor;
	
	int revision;
	
	public Version() {
		super();
	}

	public Version(int major, int minor, int revision) {
		super();
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + revision;
	}

	@Override
	public int compareTo(Version o) {
		return getVersionAsLong().compareTo(o.getVersionAsLong());
	}
	
	private Long getVersionAsLong() {
		return (long) (major*10000 + minor*100 + revision);
	}
}
