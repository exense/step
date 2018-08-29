package step.core.plans.runner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import step.core.plans.builder.PlanBuilder;
import step.core.plans.builder.PlanBuilderTest;

public class DefaultPlanRunnerTest {

	@Test
	public void test() throws IOException {
		PlanBuilder builder = PlanBuilder.create().startBlock(PlanBuilderTest.artefact("Root"));
		for(int i=1;i<1000;i++) {
			builder.add(PlanBuilderTest.artefact("Child"+i));
		}
		builder.endBlock();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		StringWriter w = new StringWriter();
		runner.run(builder.build()).printTree(w);
		
		assertEquals(readResource(getClass(),"DefaultPlanRunnerTestExpected.txt"), w.toString());
	}
	
	@Test
	public void testParallel() throws IOException, InterruptedException, ExecutionException {
		ExecutorService s = Executors.newFixedThreadPool(10);
		List<Future<?>> futures = new ArrayList<>();
		for(int i=0;i<10;i++) {
			futures.add(s.submit(()->{
				DefaultPlanRunnerTest test = new DefaultPlanRunnerTest();
				try {
					for(int j=1;j<10;j++) {
						test.test();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
		}
		s.shutdown();
		s.awaitTermination(1, TimeUnit.MINUTES);
		for (Future<?> future : futures) {
			future.get();
		}
	}
	
	public static String readResource(Class<?> clazz, String resourceName) {
		try(Scanner scanner = new Scanner(clazz.getResourceAsStream(resourceName), StandardCharsets.UTF_8.name())) {
			return scanner.useDelimiter("\\A").next();
		}
	}
}
