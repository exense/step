package step.commons.datatable;

import java.util.Date;

public class TableRow {

	private Date date;
	
	private Double value;

	public TableRow(Date date, Double value) {
		super();
		this.date = date;
		this.value = value;
	}

	public Date getDate() {
		return date;
	}

	public Double getValue() {
		return value;
	}
}
