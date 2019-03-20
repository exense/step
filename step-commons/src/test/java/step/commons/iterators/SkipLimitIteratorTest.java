package step.commons.iterators;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SkipLimitIteratorTest {

	@Test
	public void testMultipleBatches() {
		// sequence [0-22]
		List<Integer> sequence = new ArrayList<>();
		for(int i=0;i<22;i++) {
			sequence.add( i);
		}
		
		testSequence(sequence);
	}
	
	@Test
	public void testSingleElement() {
		List<Integer> sequence = new ArrayList<>();
		sequence.add(1);
		testSequence(sequence);
	}
	
	@Test
	public void testEmptySequence() {
		List<Integer> sequence = new ArrayList<>();		
		testSequence(sequence);
	}

	protected void testSequence(List<Integer> sequence) {
		SkipLimitIterator<Integer> it = new SkipLimitIterator<Integer>(new SkipLimitProvider<Integer>() {
			@Override
			public List<Integer> getBatch(int skip, int limit) {
				return sequence.subList(skip, Math.min(skip+limit, sequence.size()));
			}
		}, 10); 
		
		List<Integer> newSequence = new ArrayList<>();
		it.forEachRemaining(e->newSequence.add(e));
		
		Assert.assertArrayEquals(sequence.toArray(), newSequence.toArray());
	}
}