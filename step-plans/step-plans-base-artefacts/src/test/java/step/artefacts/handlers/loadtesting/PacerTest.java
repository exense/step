package step.artefacts.handlers.loadtesting;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import ch.exense.commons.test.categories.PerformanceTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionEngine;

public class PacerTest {

	private static final Logger logger = LoggerFactory.getLogger(PacerTest.class);

	long start = 0;

	@Test
	@Category(PerformanceTest.class)
	public void test() throws InterruptedException {
		final int durationInSeconds = 5;
		// Target throughput in 1/s
		final int expectedThroughput = 10;
		final AtomicInteger executionCount = new AtomicInteger(0);

		// Variable sleep between 3ms and 12ms
		// The variable sleep will temporarily exceed the pacing
		// of 100ms corresponding to the throughput of 10/s
		final int minSleep = 30;
		final int maxMaxSleep = 120;
		final int increment = 1;

		try (ExecutionEngine executionEngine = ExecutionEngine.builder().build()) {
			AtomicInteger atomicInteger = new AtomicInteger(minSleep);
			Pacer.scheduleAtConstantRate(i -> {
				try {
					if (start == 0) {
						start = System.currentTimeMillis();
					} else {
						long count = executionCount.get();
						printThroughput(start, count);
					}
					executionCount.incrementAndGet();
					int sleepTime = atomicInteger.getAndAdd(increment);
					logger.debug("Sleeping " + sleepTime + "ms");
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (atomicInteger.get() >= maxMaxSleep) {
					atomicInteger.set(minSleep);
				}
			}, expectedThroughput, durationInSeconds, executionEngine.newExecutionContext());
		}

		// Calculate actual throughput
		long count = executionCount.get();
		double actualThroughput = throughputInCallsPerSeconds(start, count);

		// Assert that the actual throughput is equal to the target throughput +- 1%
		double tolerance = 0.05;
		logger.info("Actual throughput [1/s]: " + actualThroughput);
		assertTrue(expectedThroughput * (1 + tolerance) > actualThroughput
				&& expectedThroughput * (1 - tolerance) < actualThroughput);
	}

	protected void printThroughput(long start, long count) {
		double throughput = throughputInCallsPerSeconds(start, count);
		logger.info("Count = " + count + ", Throughput " + throughput);
	}

	private double throughputInCallsPerSeconds(long start, long count) {
		return (1000.0 * count) / (System.currentTimeMillis() - start);
	}

}
