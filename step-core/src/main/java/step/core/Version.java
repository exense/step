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
	
	public Version(String version) {
		String[] a = version.split("\\.");
		if (a.length != 3) {
			throw new IllegalArgumentException("Invalid version format. Exepcted 3 numbers delimited by a dot but got: '" + version + "'");
		}
		try { 
			this.major = Integer.parseInt(a[0]);
			this.minor = Integer.parseInt(a[1]);
			this.revision = Integer.parseInt(a[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid version format. Exepcted 3 numbers delimited by a dot but got: '" + version + "'");
		}
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
