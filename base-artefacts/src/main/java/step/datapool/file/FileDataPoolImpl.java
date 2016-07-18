package step.datapool.file;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import step.artefacts.ForEachBlock;
import step.datapool.DataSet;


public class FileDataPoolImpl extends DataSet {
	
	ForEachBlock configuration;
	
	Iterator<String> fileIterator;
	
	String currentFile;
			
	public FileDataPoolImpl(ForEachBlock configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public void reset_() {
		File folder = new File(configuration.getFolder());
		List<String> fileList = Arrays.asList(folder.list());
		fileIterator = fileList.iterator();
	}

	@Override
	public Object next_() {
		if(fileIterator.hasNext()) {
			currentFile = fileIterator.next();
			return configuration.getFolder() + "/" + currentFile;
		} else {
			return null;
		}
	}

	@Override
	public void close() {
	}
}
