package step.commons.buffering;

import java.util.Iterator;

public class FilterIterator<T> implements Iterator<T> {

	private Iterator<T> it;
	
	private ObjectFilter<T> filter;
	
	private T nextMatch = null;
	
	public FilterIterator(Iterator<T> it, ObjectFilter<T> filter) {
		super();
		this.it = it;
		this.filter = filter;
		
		findNextMatch();
	}

	private void findNextMatch() {
		T nextElement;
		
		for(;;) {
			if(it.hasNext()) {
				nextElement = it.next();
				if(filter.matches(nextElement)) {
					nextMatch = nextElement;
					break;
				} else {
					// next
				}
			} else {
				nextMatch = null;
				break;
			}
		}
	}
	
	@Override
	public boolean hasNext() {
		return nextMatch!=null;
	}
	@Override
	public T next() {
		T result = nextMatch;
		findNextMatch();
		return result;
	}
	@Override
	public void remove() {
		throw new RuntimeException("Remove method not supported!");
	}
}
