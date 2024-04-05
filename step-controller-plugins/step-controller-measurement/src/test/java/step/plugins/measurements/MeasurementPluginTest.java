/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.measurements;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ThreadGroup;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MeasurementPluginTest extends AbstractKeyword {
	private ExecutionEngine engine;

	private static Map<String,AtomicInteger> assertMeasurementsCount = new HashMap<>();
	private static Map<String, AtomicLong> assertMeasurementsValue = new HashMap<>();

	@Before
	public void setUp() throws Exception {
		MeasurementControllerPlugin mc = new MeasurementControllerPlugin();
		mc.initGaugeCollectorRegistry(new GlobalContext());
		MeasurementPlugin.registerMeasurementHandlers(new TestMeasurementHandler());
		engine = ExecutionEngine.builder().withPlugin(new MeasurementPlugin(GaugeCollectorRegistry.getInstance()))
				.withPlugin(new FunctionPlugin()).withPlugin(new ThreadPoolPlugin())
				.withPlugin(new LocalFunctionPlugin()).withPlugin(new BaseArtefactPlugin())
				.build();
	}

	@Test
	public void test() throws IOException {		
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(10));
		threadGroup.setPacing(new DynamicValue<Integer>(10));
		threadGroup.setUsers(new DynamicValue<Integer>(5));

		AtomicInteger iterations = new AtomicInteger(0);

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
				.startBlock(threadGroup)
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
					.endBlock()
				.endBlock()
				.endBlock()
				.build();
		
		long t1 = System.currentTimeMillis();
		AtomicReference<String> execId = new AtomicReference<>();
		engine.execute(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
			execId.set(node.getExecutionID());
		});
		long t2 = System.currentTimeMillis();

		//the gauge are updated before and after so 5 threads * 2
		Assert.assertEquals(10, assertMeasurementsCount.get("ThreadGroup").get());
		Assert.assertEquals(10, assertMeasurementsCount.get("threadgroup").get());
		Assert.assertEquals(50, assertMeasurementsCount.get("TestKeywordWithMeasurements").get());
		Assert.assertEquals(50, assertMeasurementsCount.get("myMeasure1").get());
		Assert.assertEquals(150, assertMeasurementsCount.get("myMeasure2").get());
		Assert.assertEquals(50, assertMeasurementsCount.get("keyword").get());
		Assert.assertEquals(200, assertMeasurementsCount.get("custom").get());
		Assert.assertEquals(260, assertMeasurementsCount.get(execId.get()).get());
		Assert.assertEquals(5, assertMeasurementsValue.get("ThreadGroup").get());
		Assert.assertEquals(1000, assertMeasurementsValue.get("myMeasure1").get());
		Assert.assertEquals(1000, assertMeasurementsValue.get("myMeasure2").get());
	}

	public class TestMeasurementHandler implements MeasurementHandler {

		public TestMeasurementHandler() {
			GaugeCollectorRegistry.getInstance().registerHandler(this);
		}

		@Override
		public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {

		}

		@Override
		public void processMeasurements(List<Measurement> measurements) {
			synchronized (assertMeasurementsCount) {
				for (Measurement measurement : measurements) {
					incValue(measurement.getExecId(),measurement.getValue());
					incValue(measurement.getName(),measurement.getValue());
					incValue(measurement.getType(),measurement.getValue());
				}
			}
		}

		private void incValue(String key, long value) {
			if (!assertMeasurementsCount.containsKey(key)) {
				assertMeasurementsCount.put(key, new AtomicInteger());
				assertMeasurementsValue.put(key, new AtomicLong());
			}
			assertMeasurementsCount.get(key).incrementAndGet();
			long l = assertMeasurementsValue.get(key).get();
			if (l<value) {
				assertMeasurementsValue.get(key).set(value);
			}
		}

		@Override
		public void processGauges( List<Measurement> measurements) {
			processMeasurements( measurements);
		}

		@Override
		public void afterExecutionEnd(ExecutionContext context) {

		}
	}

	@Keyword
	public void TestKeywordWithMeasurements() {
		output.addMeasure("myMeasure1", 1000);
		output.addMeasure("myMeasure2", 1000);
		output.addMeasure("myMeasure2", 100);
		output.addMeasure("myMeasure2", 10);
	}
}

