package step.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IntegerSequenceIterator implements Iterator<Integer> {

	AtomicInteger counter;
	int end;
	int increment;
	
	public IntegerSequenceIterator(int start, int end, int increment) {
		counter = new AtomicInteger(start);
		this.end = end;
		this.increment = increment;
	}
	@Override
	public boolean hasNext() {
		//never called from the worker run method
		return (counter.get()<=end);
	}

	@Override
	public Integer next() {
		//must return null if reached the end
		Integer value = counter.getAndAdd(increment);
		return (value<=end) ? value : null;
	}
}
