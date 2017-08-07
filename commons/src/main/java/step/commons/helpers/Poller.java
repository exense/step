package step.commons.helpers;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class Poller {

	public static void waitFor(Supplier<Boolean> predicate, long timeout) throws TimeoutException, InterruptedException {
		long t1 = System.currentTimeMillis();
		while(System.currentTimeMillis()<t1+timeout) {
			boolean result = predicate.get();
			if(result) {
				return;
			}
			Thread.sleep(100);
		}
		throw new TimeoutException();
	}
}
