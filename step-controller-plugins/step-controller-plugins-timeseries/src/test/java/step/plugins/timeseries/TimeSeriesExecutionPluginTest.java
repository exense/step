package step.plugins.timeseries;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import junit.framework.Assert;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ThreadGroup;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.Execution;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.timeseries.TimeSeriesFilterBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationResponse;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.framework.server.ServiceRegistrationCallback;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementControllerPlugin;
import step.plugins.measurements.MeasurementPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

public class TimeSeriesExecutionPluginTest extends AbstractKeyword {

	ExecutionEngine engine;

	TimeSeriesAggregationPipeline timeSeriesAggregationPipeline;

	TimeSeriesControllerPlugin tsPlugin;
	private GlobalContext globalContext;

	@Before
	public void setUp() throws Exception {
		globalContext = new GlobalContext();
		globalContext.setCollectionFactory(new InMemoryCollectionFactory(null));
		globalContext.setServiceRegistrationCallback(new ServiceRegistrationCallback() {
			@Override
			public void register(Object o) {			}
			@Override
			public void registerService(Class<?> aClass) {			}
			@Override
			public void registerHandler(Handler handler) {			}
			@Override
			public void registerServlet(ServletHolder servletHolder, String s) {			}
			@Override
			public FilterHolder registerServletFilter(Class<? extends Filter> aClass, String s, EnumSet<DispatcherType> enumSet) {
				return null;
			}
			@Override
			public void stop() {		}
			@Override
			public void registerPackage(Package aPackage) {			}

			@Override
			public void registerWebAppRoot(String webAppRoot) {}


		});
		globalContext.put(WebApplicationConfigurationManager.class, new WebApplicationConfigurationManager());
		MeasurementControllerPlugin mc = new MeasurementControllerPlugin();
		mc.serverStart(globalContext);
		tsPlugin = new TimeSeriesControllerPlugin();
		tsPlugin.serverStart(globalContext);
		timeSeriesAggregationPipeline = globalContext.get(TimeSeriesAggregationPipeline.class);
		engine = ExecutionEngine.builder().withPlugin(new MeasurementPlugin(GaugeCollectorRegistry.getInstance()))
				.withPlugin(new FunctionPlugin()).withPlugin(new ThreadPoolPlugin())
				.withPlugin(new LocalFunctionPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TimeSeriesExecutionPlugin())
				.build();

	}

	@Test
	public void testTimeSeriesPlugins() throws InterruptedException {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(10));
		threadGroup.setPacing(new DynamicValue<Integer>(0));
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
		PlanRunnerResult planRunnerResult = engine.execute(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
			execId.set(node.getExecutionID());
		});
		long t2 = System.currentTimeMillis();
		String executionId = planRunnerResult.getExecutionId();
		Execution execution = engine.getExecutionEngineContext().getExecutionAccessor().get(executionId);
		boolean tsFlag = (boolean) execution.getCustomField(TIMESERIES_FLAG);
		Assert.assertTrue(tsFlag);

		tsPlugin.serverStop(globalContext);

		//Thread.sleep(20000);

		TimeSeriesAggregationResponse keywordsBuckets = timeSeriesAggregationPipeline
                .newQueryBuilder()
                .range(t1, t2)
                .withFilter(TimeSeriesFilterBuilder.buildFilter(Map.of("type", "keyword")))
                .withGroupDimensions(Set.of("name"))
                .build()
                .run();
		Assert.assertEquals(1,keywordsBuckets.getSeries().size());
		//assertEquals(50,keywordsBuckets.getSeries().values().stream().findFirst().get().values().stream().findFirst().get().getCount());
		TimeSeriesAggregationResponse customBuckets = timeSeriesAggregationPipeline
                .newQueryBuilder()
                .range(t1, t2)
                .withFilter(TimeSeriesFilterBuilder.buildFilter(Map.of("type", "custom")))
                .withGroupDimensions(Set.of("name","customAttr"))
                .build()
                .run();
		Assert.assertEquals(2,customBuckets.getSeries().size());
	}

	@Keyword
	public void TestKeywordWithMeasurements() {
		output.addMeasure("myMeasure1", 1000, Map.of("customAttr","val1"));
		output.addMeasure("myMeasure2", 1000,Map.of("customAttr","val2"));
		output.addMeasure("myMeasure2", 100,Map.of("customAttr","val3"));
		output.addMeasure("myMeasure2", 10,Map.of("customAttr","val4"));
	}
}
