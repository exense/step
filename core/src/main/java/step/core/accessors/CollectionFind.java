package step.core.accessors;

import java.util.Iterator;

public class CollectionFind<T> {

	long recordsTotal;
	
	long recordsFiltered;
	
	public CollectionFind(long recordsTotal, long recordsFiltered,
			Iterator<T> iterator) {
		super();
		this.recordsTotal = recordsTotal;
		this.recordsFiltered = recordsFiltered;
		this.iterator = iterator;
	}

	Iterator<T> iterator;

	public long getRecordsTotal() {
		return recordsTotal;
	}

	public long getRecordsFiltered() {
		return recordsFiltered;
	}

	public Iterator<T> getIterator() {
		return iterator;
	}
}
