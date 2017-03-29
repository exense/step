package step.datapool.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import step.datapool.DataSet;

public abstract class FileReaderDataPool extends DataSet<FileDataPool> {

	public FileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	BufferedReader br;
	String filePath;

	int lineNr;

	@Override
	public void init() {

		filePath = this.configuration.getFile().get();
		if (filePath == null || filePath.length() < 1)
			throw new RuntimeException("file path is incorrect.");
	
		FileReader in = null;
		try {
			in = new FileReader(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not open file :" + filePath + ". error was:" + e.getMessage());
		}
		br = new BufferedReader(in);
		this.lineNr = 1;
		
		doFirst_();
	}

	@Override
	public void reset() {
		throw new RuntimeException("Reset method not implemented for this DataSet type");
	}

	public abstract void doFirst_();

	@Override
	public Object next_() {
		String line = readOneLine();
		if (line == null)
			return null;
		else
			return postProcess(line);
	}
	
	protected String readOneLine(){
		String line;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read line from file " + this.filePath + ". Error was:" + e.getMessage());
		}
		this.lineNr++;
		return line;
	}

	public  abstract Object postProcess(String line);

	@Override
	public void close() {
		try {
			if(br!=null) {
				br.close();				
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not close reader properly for file " +  this.filePath + ". Error was:" + e.getMessage());
		}
	}

}
