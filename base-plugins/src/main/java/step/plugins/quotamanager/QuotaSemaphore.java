package step.plugins.quotamanager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

class QuotaSemaphore extends Semaphore {
	private static final long serialVersionUID = -3808791893658360762L;
	
	private final Object lockCounterObject = new Object();

	AtomicInteger load = new AtomicInteger(0);
	AtomicInteger peak = new AtomicInteger(0);
	
	public QuotaSemaphore(int permits) {
		super(permits);
	}

	public QuotaSemaphore(int permits, boolean fair) {
		super(permits, fair);
	}
	
	@Override
	public void acquire() throws InterruptedException {
		super.acquire();
	}

	public void decrementLoad() {
		synchronized (lockCounterObject) {
			load.decrementAndGet();
			load.set(Math.max(0, load.intValue()));
			peak.set(Math.max(peak.intValue(), load.intValue()));
		}
	}

	public int getLoad() {
		return load.intValue();
	}

	public int getPeak() {
		return peak.intValue();
	}

	public void incrementLoad() {
		synchronized (lockCounterObject) {
			load.incrementAndGet();
			peak.set(Math.max(peak.intValue(), load.intValue()));
		}
		
	}

	@Override
	public void release() {
		super.release();
	}
}