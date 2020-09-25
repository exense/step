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
package step.functions.type;

public class FunctionTypeConfiguration {
	
	private int fileResolverCacheConcurrencyLevel = 4;
	
	private int fileResolverCacheMaximumsize = 1000;
	
	private int fileResolverCacheExpireAfter = 500;

	public int getFileResolverCacheConcurrencyLevel() {
		return fileResolverCacheConcurrencyLevel;
	}

	public void setFileResolverCacheConcurrencyLevel(int fileResolverCacheConcurrencyLevel) {
		this.fileResolverCacheConcurrencyLevel = fileResolverCacheConcurrencyLevel;
	}

	public int getFileResolverCacheMaximumsize() {
		return fileResolverCacheMaximumsize;
	}

	public void setFileResolverCacheMaximumsize(int fileResolverCacheMaximumsize) {
		this.fileResolverCacheMaximumsize = fileResolverCacheMaximumsize;
	}

	public int getFileResolverCacheExpireAfter() {
		return fileResolverCacheExpireAfter;
	}

	public void setFileResolverCacheExpireAfter(int fileResolverCacheExpireAfter) {
		this.fileResolverCacheExpireAfter = fileResolverCacheExpireAfter;
	}
}
