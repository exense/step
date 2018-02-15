package step.datapool.file;

import java.util.HashSet;
import java.util.Set;

import step.core.variables.SimpleStringMap;

public class FlatFileReaderDataPool extends FileReaderDataPool {

	public FlatFileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	@Override
	public Object postProcess(String line) {
		return new FlatLineRowWrapper(line);
	}

	public class FlatLineRowWrapper extends SimpleStringMap {

		String line;
		
		public FlatLineRowWrapper(String line) {
			super();
			this.line = line;
		}

		@Override
		public String get(String key) {
			return line;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public String put(String key, String value) {
			throw new RuntimeException("Put not supported on flat file data pool");
		}

		@Override
		public Set<String> keySet() {
			Set<String> keySet = new HashSet<>();
			keySet.add("row");
			return keySet;
		}
	}

	@Override
	public void doFirst_() {}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}