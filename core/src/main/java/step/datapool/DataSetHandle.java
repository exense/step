package step.datapool;

public interface DataSetHandle {

	public Object next();
	
	public void addRow(Object row);
}
