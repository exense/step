package step.datapool.file;

public class FlatFileReaderDataPool extends FileReaderDataPool {

	public FlatFileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	@Override
	public Object postProcess(String line) {
		return line;
	}
	
	@Override
	public void doFirst_() {}
	
	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}