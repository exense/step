/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
	
	public static Version getCurrentVersion() {
		// TODO read this from manifest
		return new Version(3,19,0);
	}
}
