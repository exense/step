package step.datapool.file;

import java.util.HashMap;
import java.util.Vector;

import step.core.variables.SimpleStringMap;

public class CSVReaderDataPool extends FileReaderDataPool {
	
	Vector<String> headers;
	String delimiter;
		
	public CSVReaderDataPool(CSVDataPool configuration) {
		super(configuration);
		this.delimiter = configuration.getDelimiter().get();
	}

	@Override
	public Object postProcess(String line) {
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		Vector<String> csv = splitCSV(line); 
		for(int i = 0; i< csv.size(); i++){
			map.put(headers.get(i), csv.get(i));
		}

		return new CSVRowWrapper(super.lineNr, map);
	}

	public class CSVRowWrapper extends SimpleStringMap {

		private HashMap<String,Object> rowData;

		public CSVRowWrapper(int rowNum, HashMap<String,Object> row) {
			super();

			if(rowNum < 1)
				throw new RuntimeException("Invalid row number:" + rowNum);
			this.rowData = row; 
		}

		@Override
		public String put(String key, String value){
			throw new RuntimeException("Put into a CSVRowWrapper row is currently not supported.");
		}

		@Override
		public String get(String key) {
			return (String) rowData.get(key);
		}

	}

	public Vector<String> getHeaders(String readOneLine) {
		return splitCSV(readOneLine);
	}
	
	public Vector<String> splitCSV(String readOneLine) {
		Vector<String> v = new Vector<String>();
		for(String s : readOneLine.split(this.delimiter))
			v.add(s);
		
		return v;
	}
	
	@Override
	public void doFirst_() {
		this.headers = getHeaders(readOneLine());
	}
}
