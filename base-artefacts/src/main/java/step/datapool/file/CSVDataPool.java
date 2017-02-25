package step.datapool.file;

import step.core.dynamicbeans.DynamicValue;

public class CSVDataPool extends FileDataPool {
	
	DynamicValue<String> delimiter = new DynamicValue<String>(",");

	public CSVDataPool() {
		super();
	}

	public DynamicValue<String> getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(DynamicValue<String> delimiter) {
		this.delimiter = delimiter;
	}
}
