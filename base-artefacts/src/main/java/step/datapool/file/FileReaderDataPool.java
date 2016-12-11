package step.datapool.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import step.artefacts.ForEachBlock;
import step.datapool.DataSet;

public abstract class FileReaderDataPool extends DataSet {

	ForEachBlock configuration;
	BufferedReader br;
	String filePath;

	int lineNr;

	public FileReaderDataPool(ForEachBlock forEach){

		this.configuration = forEach;
	}

	@Override
	public void reset_() {

		filePath = this.configuration.getTable();
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
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not close reader properly for file " +  this.filePath + ". Error was:" + e.getMessage());
		}
	}

}
