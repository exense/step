package step.datapool.file;

import step.core.variables.SimpleStringMap;

public class FlatFileReaderDataPool extends FileReaderDataPool {

	public FlatFileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	@Override
	public Object postProcess(String line) {
		return new FlatLineRowWrapper(super.lineNr, line);
	}

	@SuppressWarnings("serial")
	public class FlatLineRowWrapper extends SimpleStringMap {

		public FlatLineRowWrapper(int rowNum, String line) {
			super();

			if(rowNum < 1)
				throw new RuntimeException("Invalid row number:" + rowNum);
			
			// we only really have one key and value in a "flat file" type of logic
			put("default", line); 
		}

		@Override
		public String get(String key) {
			// get our only value, i.e the whole file
			return get("default");
		}

		@Override
		public int size() {
			return size();
		}

		@Override
		public boolean isEmpty() {
			return isEmpty();
		}

		@Override
		public String put_(String key, String value) {
			return put(key,value);
		}

	}

	@Override
	public void doFirst_() {}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}