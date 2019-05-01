package step.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntegerSequenceIterator implements Iterator<Integer> {

	private Iterator<Integer> iterator;
	
	public IntegerSequenceIterator(int start, int end, int inrement) {
		List<Integer> sequence = new ArrayList<>();
		for(int i=start; i<=end; i+=inrement) {
			sequence.add(i);
		}
		iterator = sequence.iterator();
	}
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Integer next() {
		return iterator.next();
	}
}
