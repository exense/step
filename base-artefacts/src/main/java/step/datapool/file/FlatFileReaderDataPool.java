package step.datapool.file;

import java.util.HashMap;

import step.core.variables.SimpleStringMap;

public class FlatFileReaderDataPool extends FileReaderDataPool {

	public FlatFileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	@Override
	public Object postProcess(String line) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("default", line);

		return new FlatLineRowWrapper(super.lineNr, map);
	}

	public class FlatLineRowWrapper extends SimpleStringMap {

		private HashMap<String,Object> rowData;

		public FlatLineRowWrapper(int rowNum, HashMap<String,Object> row) {
			super();

			if(rowNum < 1)
				throw new RuntimeException("Invalid row number:" + rowNum);
			this.rowData = row; 
		}

		@Override
		public String put(String key, String value){
			throw new RuntimeException("Put into a FlatFileReader row is currently not supported.");
		}

		@Override
		public String get(String key) {

			return (String) rowData.get("default");
		}

		@Override
		public int size() {
			return rowData.size();
		}

		@Override
		public boolean isEmpty() {
			return rowData.isEmpty();
		}

	}

	@Override
	public void doFirst_() {}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}