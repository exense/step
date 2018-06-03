package step.commons.iterators;

import java.util.Iterator;
import java.util.List;

public class SkipLimitIterator<T> implements Iterator<T> {

	private final int batchSize;
	
	private final SkipLimitProvider<T> provider;

	private Iterator<T> currentBatchIt = null;
	private int currentBatchCount = 0;
	private int currentSkip = 0;
	
	private T next = null;
	
	public SkipLimitIterator(SkipLimitProvider<T> provider) {
		this(provider, 1000);
	}
	
	public SkipLimitIterator(SkipLimitProvider<T> provider, int batchSize) {
		super();
		this.provider = provider;
		this.batchSize = batchSize;
		getNextBatch();
		preloadNextElement();
	}

	protected void preloadNextElement() {
		if(currentBatchIt.hasNext()) {
			currentBatchCount++;
			next = currentBatchIt.next();
		} else {
			if(currentBatchCount>=batchSize) {
				getNextBatch();
				preloadNextElement();
			} else {
				next = null;
			}
		}
	}

	protected void getNextBatch() {
		List<T> batch = provider.getBatch(currentSkip, batchSize);
		if(batch.size()>batchSize) {
			throw new RuntimeException("The size of the batch returned by the SkipLimitProvider is higher than the specified batch size. Expected size was "
											+batchSize+". Actual batch size was "+batch.size());
		}
		currentBatchIt = batch.iterator();
		currentBatchCount = 0;
		currentSkip+=batchSize;
	}
	
	@Override
	public boolean hasNext() {
		return next!=null;
	}

	@Override
	public T next() {
		T result = next;
		preloadNextElement();
		return result;
	}
}
