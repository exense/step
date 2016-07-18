package step.commons.datatable;

import java.util.ArrayList;
import java.util.List;

public class DataTable {
	
	private List<TableRow> rows = new ArrayList<>();

	public boolean addRow(TableRow e) {
		return rows.add(e);
	}
	
	public boolean addRows(List<TableRow> e) {
		return rows.addAll(e);
	}


	public List<TableRow> getRows() {
		return rows;
	}

}
