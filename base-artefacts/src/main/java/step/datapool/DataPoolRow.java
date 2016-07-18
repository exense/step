package step.datapool;

public class DataPoolRow {
	
	private final int rowNum;
	
	private final Object value;

	public DataPoolRow(int rowNum, Object value) {
		super();
		this.rowNum = rowNum;
		this.value = value;
	}

	public int getRowNum() {
		return rowNum;
	}

	public Object getValue() {
		return value;
	}

}
