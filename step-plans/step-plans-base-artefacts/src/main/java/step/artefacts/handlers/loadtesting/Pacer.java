package step.artefacts.handlers.loadtesting;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pacer {

	private static final int MAX_ACCUMULATIONS = 100;
	private static final Logger logger = LoggerFactory.getLogger(Pacer.class);

	public static void scheduleAtConstantRate(Consumer<Integer> r, long executionsPerSecond, long maxDurationInSeconds) throws InterruptedException {
		scheduleAtConstantPacing(r, 1000/executionsPerSecond, maxDurationInSeconds);
	
	}
	
	public static void scheduleAtConstantPacing(Consumer<Integer> r, long pacingMs, long maxDurationInSeconds) throws InterruptedException {
		long maxDurationInMs = 1000 * maxDurationInSeconds;
		scheduleAtConstantPacing(r, pacingMs, c -> c.getDuration() < maxDurationInMs);
	}
	
	public static class Context {
		
		private long duration;
		private long iterations;
		
		public boolean update(long duration, long iterations) {
			this.duration = duration;
			this.iterations = iterations;
			return true;
		}

		public long getDuration() {
			return duration;
		}

		public long getIterations() {
			return iterations;
		}
	}
	
	public static void scheduleAtConstantPacing(Consumer<Integer> r, long pacingMs, Predicate<Context> predicate) throws InterruptedException {
		long start = System.currentTimeMillis();
		long pacingNs = pacingMs * 1000000;
		long accumulatedDelay = 0;
		int accumulationCounter = 0;
		
		long t2=System.nanoTime();
		long expectedSleepOfPreviousIterationNs=0;
		int iterationCount = 0;
		Context context = new Context();
		while (context.update(System.currentTimeMillis()-start, iterationCount++) && predicate.test(context)) {
			long actualSleepOfPreviousIterationNs = System.nanoTime()-t2;
			long sleepCorrection = actualSleepOfPreviousIterationNs-expectedSleepOfPreviousIterationNs;
			long t1 = System.nanoTime();
			r.accept(iterationCount);
			t2 = System.nanoTime();
			long duration = t2 - t1;
			long correctedDuration = duration + sleepCorrection;
			if (isDebugEnabled()) {
				debug("Executed in "+duration+"ns. Corrected duration "+correctedDuration+"ns");
			}
			long difference = pacingNs - correctedDuration;

			long sleepTime;
			if (difference < 0) {
				// Pacing exceeded => accumulating delay
				accumulatedDelay -= difference;
				accumulationCounter++;
				sleepTime = 0;
				if (isDebugEnabled()) {
					debug("Pacing exceeded. Accumulating " + -difference
							+ "ns of delay. Total accumulated delay: " + accumulatedDelay + "ns");
				}
			} else {
				// Execution faster than pacing => try to catch up
				sleepTime = Math.max(0, difference - accumulatedDelay);
				long caughtUpTime = difference - sleepTime;
				accumulatedDelay -= caughtUpTime;
				if (isDebugEnabled()) {
					debug("Execution " + difference + "ns faster than pacing. "
							+ (caughtUpTime > 0 ? "Caught " + caughtUpTime + "ns up." : "")+". Total accumulated delay: " + accumulatedDelay + "ns");
				}
			}

			if (accumulationCounter > 0 && accumulatedDelay == 0) {
				// Delay caught up. Reset accumulation counter
				accumulationCounter = 0;
				if (isDebugEnabled()) {
					debug("Delay caught up. Resetting accumulation counter");
				}
			}

			if (accumulationCounter > MAX_ACCUMULATIONS) {
				if (isDebugEnabled()) {
					debug("Accumulated delay during " + MAX_ACCUMULATIONS
							+ " consecutive iterations. Unlikely to catch up. Resetting accumulation counter");
				}
				accumulationCounter = 0;
				accumulatedDelay = 0;
			}
			
			long sleepTimeMs = sleepTime/1000000;
			if (isDebugEnabled()) {
				debug("Sleeping "+sleepTimeMs+"ms");
			}
			Thread.sleep(sleepTimeMs);
			expectedSleepOfPreviousIterationNs = sleepTime;
		}
		
	}

	protected static boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	protected static void debug(String message) {
		logger.debug(message);
	}
}
