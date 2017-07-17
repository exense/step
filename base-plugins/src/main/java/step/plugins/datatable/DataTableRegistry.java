package step.plugins.datatable;

import java.util.HashMap;
import java.util.Map;

public class DataTableRegistry {

	Map<String, BackendDataTable> tables = new HashMap<>();	
	

	public BackendDataTable addTable(String key, BackendDataTable value) {
		return tables.put(key, value);
	}
	
	public BackendDataTable getTable(String key) {
		return tables.get(key);
	}
}
