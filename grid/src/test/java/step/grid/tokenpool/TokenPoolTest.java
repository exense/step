package step.grid.tokenpool;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;


public class TokenPoolTest {
	
	private static final Logger logger = LoggerFactory.getLogger(TokenPoolTest.class);
	
	@Test
	public void test_Match_Positive_1() throws Exception {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest(Pattern.compile("red"), true));
		
		IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 10);
		Assert.assertNotNull(selectedIdentityImpl);
		
	}
	
	@Test
	public void test_Match_Positive_2() throws Exception {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		//token.addInterest(new Se)
		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addAttribute("color", "green");
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		
		IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 10);
		Assert.assertNotNull(selectedIdentityImpl);
	}
	
	@Test
	public void test_Match_Negative_2() {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		//token.addInterest(new Se)
		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addAttribute("color", "yellow");
		pretender.addInterest("color",new Interest( Pattern.compile("green"), true));
		
		IdentityImpl selectedIdentityImpl;
		Exception e1 = null;
		try {
			selectedIdentityImpl = pool.selectToken(pretender, 10);
		} catch (Exception e) {
			e1 = e;
		}
		Assert.assertNotNull(e1);
		
	}
	
	@Test
	public void test_Match_Preference_1() throws Exception {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		pool.offerToken(token);		
		
		token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "triangle");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addAttribute("color", "green");
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		pretender.addInterest("shape",new Interest( Pattern.compile("circle"), false));
		
		IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 100);
		Assert.assertNotNull(selectedIdentityImpl);
		Assert.assertEquals("red", selectedIdentityImpl.getAttributes().get("color"));
		Assert.assertEquals("circle", selectedIdentityImpl.getAttributes().get("shape"));
		
	}
	
	@Test
	public void test_Match_Preference_2() throws Exception {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		token.addAttribute("id", "1");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		pool.offerToken(token);		
		
		token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		token.addAttribute("id", "2");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		token.addInterest("shape",new Interest( Pattern.compile("line"), false));
		pool.offerToken(token);		
		
		token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "triangle");
		token.addAttribute("id", "3");
		token.addInterest("color",new Interest( Pattern.compile("green"), true));
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addAttribute("color", "green");
		pretender.addAttribute("shape", "line");
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		pretender.addInterest("shape",new Interest( Pattern.compile("circle"), false));
		
		IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 100);
		Assert.assertNotNull(selectedIdentityImpl);
		Assert.assertEquals("red", selectedIdentityImpl.getAttributes().get("color"));
		Assert.assertEquals("circle", selectedIdentityImpl.getAttributes().get("shape"));
		Assert.assertEquals("2", selectedIdentityImpl.getAttributes().get("id"));
		
	}
	
	@Test
	public void test_Pool_Select() {
		TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		//token.addInterest(new Se)
		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("green"), true));
		
		IdentityImpl selectedIdentityImpl;
		Exception e1 = null;
		try {
			selectedIdentityImpl = pool.selectToken(pretender, 10);
		} catch (Exception e) {
			e1 = e;
		}
		Assert.assertNotNull(e1);
		
	}
	
	@Test
	public void test_Pool_Invalidate_1() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool<>(new SimpleAffinityEvaluator());
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("id", "1");
		
		pool.offerToken(token);
		
		pool.invalidateToken(token);
		
		Assert.assertEquals(0, pool.getSize());
	}
	
	@Test
	public void test_Pool_Invalidate_2() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool<>(new SimpleAffinityEvaluator());
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("id", "1");
		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("id",new Interest(Pattern.compile("1"), true));
		
		IdentityImpl selectedToken = pool.selectToken(pretender, 10);
		pool.invalidateToken(selectedToken);
		
		pool.returnToken(token);
		
		Assert.assertEquals(0, pool.getSize());
	}
	
	@Test
	public void test_Pool_Invalidate_3() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool<>(new SimpleAffinityEvaluator());
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("id", "1");
		
		pool.offerToken(token);
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("id",new Interest(Pattern.compile("1"), true));
		
		IdentityImpl selectedToken = pool.selectToken(pretender, 10);
		pool.invalidate(selectedToken.getID());
		
		pool.returnToken(token);
		
		Assert.assertEquals(0, pool.getSize());
	}
	
	@Test
	public void test_Pool_NotifyAfterTokenRemove() throws InterruptedException {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		//token.addInterest(new Se)
		
		pool.offerToken(token);
		
		final IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		
		IdentityImpl selectedIdentityImpl = null;
		Exception e1 = null;
		try {
			selectedIdentityImpl = pool.selectToken(pretender, 10);
		} catch (Exception e) {
			e1 = e;
		}
		Assert.assertNotNull(selectedIdentityImpl);
		Assert.assertNull(e1);
		
		final List<Exception> l = new ArrayList<>();
		
		final AtomicBoolean tokenInvalidated = new AtomicBoolean(false);
		
		Thread t = new Thread() {

			@Override
			public void run() {
				IdentityImpl selectedIdentityImpl;
				try {
					selectedIdentityImpl = pool.selectToken(pretender, 0, 1);
				} catch (TimeoutException e) {
					if(tokenInvalidated.get()) {
						l.add(e);
					} else {
						l.add(new Exception("Timeout occurred before token invalidation"));
					}
				} catch (Exception e) {
					l.add(e);
				}
			}
			
		};
		
		t.start();
		Thread.currentThread().sleep(10);
		tokenInvalidated.set(true);
		pool.invalidateToken(token);
		pool.returnToken(token);
		Thread.currentThread().sleep(100);
		Assert.assertEquals(1, l.size());
		Assert.assertTrue(l.get(0) instanceof TimeoutException);
		
	}
	
	@Test
	public void test_Pool_WaitingQueue() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		//token.addInterest(new Se)
		
		(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				pool.offerToken(token);
			}
		}).start();
		
		IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));

		IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 1000);
		Assert.assertNotNull(selectedIdentityImpl);
		
	}
	
	@Test
	public void test_Pool_Parallel_1TokenPerThread() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		final IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		
		final int testDurationMs = 1000;
		final int nThreads = 10;
		
		
		ExecutorService e = Executors.newFixedThreadPool(nThreads+1);
		
		long t1 = System.currentTimeMillis();
		List<Future<Boolean>> futures = new ArrayList<>();
		for(int i=0;i<nThreads;i++) {
			final IdentityImpl token = new IdentityImpl();
			token.addAttribute("color", "red");
			token.addAttribute("shape", "circle");
			pool.offerToken(token);
			
			
			Future<Boolean> f = e.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					for(int i=0;i<testDurationMs;i++) {
						IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, nThreads + 5);
						try {
							Assert.assertNotNull(selectedIdentityImpl);
							Assert.assertFalse(selectedIdentityImpl.used);
							selectedIdentityImpl.used = true;
							
							Thread.sleep(1);
						} catch (Throwable e) {
							logger.error("Unexpected error", e);
						} finally {
							selectedIdentityImpl.used = false;
							pool.returnToken(selectedIdentityImpl);
						}
					}
					return true;
				}
			});
			futures.add(f);
		}
		
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "blue");
		token.addAttribute("shape", "circle");
		
		final int periodNs = 1000;
		Future offeringThread = e.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				long t2 = System.currentTimeMillis();
				for(int i=0;i<testDurationMs*1000/periodNs;i++) {
					pool.offerToken(token);
					Thread.sleep(0, periodNs);
				}
				System.out.println("Duration offer token: " + (System.currentTimeMillis()-t2));
				return true;
			}
		});
		
		for(Future<Boolean> f:futures) {
			f.get();			
		}
		
		long duration = (System.currentTimeMillis()-t1);
		System.out.println("Duration: " + duration);

		System.out.println("testDurationMs: " + testDurationMs);
		//Assert.assertTrue(duration<testDurationMs + 500);
		
		offeringThread.get();
		
		e.shutdown();
		
		Assert.assertEquals(11, pool.getSize());
	}

	
	
	@Test
	public void test_Pool_Parallel() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		pool.offerToken(token);
		
		int otherTokenCount = 5;
		
		for(int i=0;i<otherTokenCount;i++) {
			IdentityImpl otherToken = new IdentityImpl();
			otherToken.addAttribute("color", "red");
			otherToken.addAttribute("shape", "circle");
			pool.offerToken(otherToken);
		}
		//token.addInterest(new Se)
		
		final IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		
		final int testDurationMs = 1000;
		final int nThreads = 10;
		
		ExecutorService e = Executors.newFixedThreadPool(nThreads+1);
		
		long t1 = System.currentTimeMillis();
		List<Future<Boolean>> futures = new ArrayList<>();
		for(int i=0;i<nThreads;i++) {
			Future<Boolean> f = e.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					for(int i=0;i<testDurationMs/nThreads;i++) {
						IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, nThreads + 20);
						try {
							Assert.assertNotNull(selectedIdentityImpl);
							Assert.assertFalse(selectedIdentityImpl.used);
							selectedIdentityImpl.used = true;
							
							Thread.sleep(1);
						} catch (Throwable e) {
							logger.error("Unexpected error", e);
						} finally {
							selectedIdentityImpl.used = false;
							pool.returnToken(selectedIdentityImpl);
						}
					}
					return true;
				}
			});
			futures.add(f);
		}
		
		
		final int periodNs = 1000;
		Future offeringThread = e.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				long t2 = System.currentTimeMillis();
				for(int i=0;i<testDurationMs*1000/periodNs;i++) {
					pool.offerToken(token);
					Thread.sleep(0, periodNs);
				}
				System.out.println("Duration offer token: " + (System.currentTimeMillis()-t2));
				return true;
			}
		});
		
		for(Future<Boolean> f:futures) {
			f.get();			
		}
		
		long duration = (System.currentTimeMillis()-t1);
		System.out.println("Duration: " + duration);

		System.out.println("testDurationMs: " + testDurationMs);
		Assert.assertTrue(duration<testDurationMs + 100);
		
		offeringThread.get();
		
		e.shutdown();
		
		Assert.assertEquals(1+otherTokenCount, pool.getSize());
		for(Token<?> t:pool.getTokens()) {
			Assert.assertTrue(t.isFree());
		}		
	}
	
	public void test_Pool_Perf_Poolsize() throws Exception {
		final TokenPool<IdentityImpl, IdentityImpl> pool = new TokenPool(new SimpleAffinityEvaluator());
		
		final IdentityImpl pretender = new IdentityImpl();
		pretender.addInterest("color",new Interest( Pattern.compile("red"), true));
		
		final int nIterations = 1000;
		final int poolSize = 10000; 
		
		for(int i=0;i<poolSize;i++) {
			final IdentityImpl token = new IdentityImpl();
			token.addAttribute("color", UUID.randomUUID().toString());
			token.addAttribute("shape", "circle");
			pool.offerToken(token);
		}
		
		final IdentityImpl token = new IdentityImpl();
		token.addAttribute("color", "red");
		token.addAttribute("shape", "circle");
		pool.offerToken(token);
		
		long t1 = System.currentTimeMillis();
		
		for(int i=0;i<nIterations;i++) {
			IdentityImpl selectedIdentityImpl = pool.selectToken(pretender, 1000);
			try {
				Assert.assertNotNull(selectedIdentityImpl);
				Assert.assertFalse(selectedIdentityImpl.used);
				selectedIdentityImpl.used = true;
			} catch (Throwable e) {
				logger.error("Unexpected error", e);
			} finally {
				selectedIdentityImpl.used = false;
				pool.returnToken(selectedIdentityImpl);
			}
		}
		
		long duration = (System.currentTimeMillis()-t1);
		double durationPerSelection = duration / (1.0*nIterations);
		System.out.println("Duration: " + duration);
		System.out.println("Duration per iteration [ms]: " + durationPerSelection);

		//Assert.assertTrue(duration<testDurationMs + 100);
	}

}
