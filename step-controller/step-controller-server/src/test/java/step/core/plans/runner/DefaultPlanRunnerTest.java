package step.core.plans.runner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

public class DefaultPlanRunnerTest {

	@Test
	public void test() throws IOException {
		PlanBuilder builder = PlanBuilder.create().startBlock(BaseArtefacts.sequence());
		for(int i=1;i<1000;i++) {
			builder.add(BaseArtefacts.sequence());
		}
		builder.endBlock();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		PlanRunnerResult result = runner.run(builder.build());
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		PlanRunnerResultAssert.assertEquals(this.getClass(), "DefaultPlanRunnerTestExpected.txt", result);
	}
	
//	Commenting this test out as the initialization of the DefaultPlanRunner is taking much more time since it is using the ExecutionEngine
//  The DefaultPlanRunner is deprecated and will be removed in future releases
//	
//	@Test
//	public void testParallel() throws IOException, InterruptedException, ExecutionException {
//		ExecutorService s = Executors.newFixedThreadPool(10);
//		List<Future<?>> futures = new ArrayList<>();
//		for(int i=0;i<10;i++) {
//			futures.add(s.submit(()->{
//				DefaultPlanRunnerTest test = new DefaultPlanRunnerTest();
//				try {
//					for(int j=1;j<10;j++) {
//						test.test();
//					}
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			}));
//		}
//		s.shutdown();
//		s.awaitTermination(1, TimeUnit.MINUTES);
//		for (Future<?> future : futures) {
//			future.get();
//		}
//	}
}
