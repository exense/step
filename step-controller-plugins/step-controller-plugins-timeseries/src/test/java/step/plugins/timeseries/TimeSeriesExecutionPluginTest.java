package step.plugins.timeseries;

import junit.framework.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ThreadGroup;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.Execution;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.planbuilder.BaseArtefacts;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

class TimeSeriesExecutionPluginTest {

	ExecutionEngine engine;
	@BeforeEach
	void setUp() {
		engine = ExecutionEngine.builder().withPlugin(new MeasurementPlugin(GaugeCollectorRegistry.getInstance()))
				.withPlugin(new FunctionPlugin()).withPlugin(new ThreadPoolPlugin())
				.withPlugin(new LocalFunctionPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TimeSeriesExecutionPlugin())
				.build();
	}

	@Test
	void initializeExecutionContext() {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(10));
		threadGroup.setPacing(new DynamicValue<Integer>(10));
		threadGroup.setUsers(new DynamicValue<Integer>(5));

		AtomicInteger iterations = new AtomicInteger(0);

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
				.endBlock()
				.build();

		long t1 = System.currentTimeMillis();
		AtomicReference<String> execId = new AtomicReference<>();
		PlanRunnerResult planRunnerResult = engine.execute(plan).visitReportNodes(node -> {
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
			execId.set(node.getExecutionID());
		});
		String executionId = planRunnerResult.getExecutionId();
		Execution execution = engine.getExecutionEngineContext().getExecutionAccessor().get(executionId);
		boolean tsFlag = (boolean) execution.getCustomField(TIMESERIES_FLAG);
		Assert.assertTrue(tsFlag);
		long t2 = System.currentTimeMillis();
	}
}