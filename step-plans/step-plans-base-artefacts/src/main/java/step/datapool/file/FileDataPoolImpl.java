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
package step.datapool.file;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import step.datapool.DataSet;


public class FileDataPoolImpl extends DataSet<DirectoryDataPool> {
	
	public FileDataPoolImpl(DirectoryDataPool configuration) {
		super(configuration);
	}
		
	Iterator<File> fileIterator;
	
	File currentFile;

	private File folder;
	
	@Override
	public void init() {
		super.init();
		folder = new File(configuration.getFolder().get());
		initFileIterator();
	}

	private void initFileIterator() {
		List<File> fileList = Arrays.asList(folder.listFiles());
		fileIterator = fileList.iterator();
	}
	
	@Override
	public void reset() {
		initFileIterator();
	}
	
	public class ExtendedFile {
		
		private final File file;

		public ExtendedFile(File file) {
			super();
			this.file = file;
		}
		
		public String getName() {
			return file.getName();
		}
		
		public String getNameWithoutExtension() {
			String fname = getName();
			int id = fname.lastIndexOf('.'); 
			return id >= 0 ? fname.substring(0, id) : fname;
		}
		
		public String getPath() {
			return file.getParent();
		}

		@Override
		public String toString() {
			return file.getAbsolutePath();
		}
		
		
	}

	@Override
	public Object next_() {
		if(fileIterator.hasNext()) {
			return new ExtendedFile(fileIterator.next());
		} else {
			return null;
		}
	}

	@Override
	public void close() {
		super.close();
	}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}
