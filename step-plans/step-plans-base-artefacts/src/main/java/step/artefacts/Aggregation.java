package step.artefacts;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

import step.core.reports.Measure;

public class Aggregation {

	private final String name;
	private AtomicLong count = new AtomicLong();
	private AtomicLong sum = new AtomicLong();
	private LongAccumulator max = new LongAccumulator(Long::max, 0L);
	private LongAccumulator min = new LongAccumulator(Long::min, Long.MAX_VALUE);

	
	public Aggregation(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public final void addMeasure(Measure measure) {
		count.incrementAndGet();
		long duration = measure.getDuration();
		sum.addAndGet(duration);
		max.accumulate(duration);
		min.accumulate(duration);
	}

	public long getCount() {
		return count.longValue();
	}

	public long getSum() {
		return sum.longValue();
	}
	
	public long getMax() {
		return max.longValue();
	}
	
	public long getMin() {
		return min.longValue();
	}
	
	public long getAvg() {
		return getSum()/getCount();
	}
}
