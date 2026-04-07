package step.plugins.timeseries;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.websocket.server.ServerEndpointConfig;
import org.junit.Assert;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ThreadGroup;
import step.core.GlobalContext;
import step.core.GlobalContextBuilder;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.Execution;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesFilterBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationPipeline;
import step.core.timeseries.aggregation.TimeSeriesAggregationQuery;
import step.core.timeseries.aggregation.TimeSeriesAggregationQueryBuilder;
import step.core.timeseries.aggregation.TimeSeriesAggregationResponse;
import step.core.views.ViewPlugin;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.framework.server.ServiceRegistrationCallback;
import step.framework.server.tables.TableRegistry;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.migration.MigrationManager;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.SamplesControllerPlugin;
import step.plugins.measurements.SamplesExecutionPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static step.plugins.timeseries.TimeSeriesControllerPlugin.*;
import static step.plugins.timeseries.TimeSeriesExecutionPlugin.TIMESERIES_FLAG;

public class TimeSeriesExecutionPluginTest extends AbstractKeyword {

    ExecutionEngine engine;

    TimeSeriesAggregationPipeline timeSeriesAggregationPipeline;

    TimeSeriesControllerPlugin tsPlugin;
    private GlobalContext globalContext;

    public enum Mode {
        INCLUDED_ATTRIBUTES,
        EXCLUDED_ATTRIBUTES,
        NONE
    }


    public void setUp(Mode timeSeriesAttributeMode) throws Exception {
        globalContext = GlobalContextBuilder.createGlobalContext();
        globalContext.put(MigrationManager.class, new MigrationManager());
        globalContext.setEntityManager(new EntityManager());
        globalContext.put(TableRegistry.class, new TableRegistry());
        globalContext.setCollectionFactory(new InMemoryCollectionFactory(null));
        globalContext.setServiceRegistrationCallback(new ServiceRegistrationCallback() {
            @Override
            public void register(Object o) {
            }

            @Override
            public void registerService(Class<?> aClass) {
            }

            @Override
            public void registerHandler(Handler handler) {
            }

            @Override
            public void registerServlet(ServletHolder servletHolder, String s) {
            }

            @Override
            public FilterHolder registerServletFilter(Class<? extends Filter> aClass, String s, EnumSet<DispatcherType> enumSet) {
                return null;
            }

            @Override
            public void stop() {
            }

            @Override
            public void registerPackage(Package aPackage) {
            }

            @Override
            public void registerWebAppRoot(String webAppRoot) {
            }

            @Override
            public void registerWebsocketEndpoint(ServerEndpointConfig serverEndpointConfig) {
            }
        });
        globalContext.put(WebApplicationConfigurationManager.class, new WebApplicationConfigurationManager());
        SamplesControllerPlugin mc = new SamplesControllerPlugin();
        //Legacy mode before Step 30
        switch (timeSeriesAttributeMode) {
            case INCLUDED_ATTRIBUTES:
                globalContext.getConfiguration().putProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, TIME_SERIES_ATTRIBUTES_DEFAULT);
                globalContext.getConfiguration().putProperty(TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY, "");
                break;
            case EXCLUDED_ATTRIBUTES:
                globalContext.getConfiguration().putProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, "");
                globalContext.getConfiguration().putProperty(TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY, "customAttr");
                break;
            case NONE:
                globalContext.getConfiguration().putProperty(TIME_SERIES_ATTRIBUTES_PROPERTY, "");
                globalContext.getConfiguration().putProperty(TIME_SERIES_EXCLUDED_ATTRIBUTES_PROPERTY, "");
                break;
            default:
                throw new IllegalArgumentException("Invalid mode " + timeSeriesAttributeMode);
        }
        mc.serverStart(globalContext);
        tsPlugin = new TimeSeriesControllerPlugin();
        tsPlugin.serverStart(globalContext);
        TimeSeries timeSeries = globalContext.get(TimeSeries.class);
        timeSeriesAggregationPipeline = globalContext.get(TimeSeriesAggregationPipeline.class);
        engine = ExecutionEngine.builder()
            .withPlugin(new SamplesExecutionPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new LocalFunctionPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new ExecutionTypePlugin())
            .withPlugin(new ViewPlugin())
            .withPlugin(new TimeSeriesExecutionPlugin(timeSeries))
            .withPlugin(new ViewPlugin())
            .build();

    }


    @Test
    public void testTimeSeriesPluginsWithIncludedAttributes() throws Exception {
        testTimeSeriesPlugins(Mode.INCLUDED_ATTRIBUTES);
    }

    @Test
    public void testTimeSeriesPluginsWithExcludedAttributes() throws Exception {
        testTimeSeriesPlugins(Mode.EXCLUDED_ATTRIBUTES);
    }

    @Test
    public void testTimeSeriesPluginsWithoutInclusionAndExclusion() throws Exception {
        testTimeSeriesPlugins(Mode.NONE);
    }

    public void testTimeSeriesPlugins(Mode timeSeriesAttributeMode) throws Exception {
        setUp(timeSeriesAttributeMode);
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
        PlanRunnerResult planRunnerResult = engine.execute(plan).visitReportNodes(node -> {
            Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
            execId.set(node.getExecutionID());
        });
        long t2 = System.currentTimeMillis();
        String executionId = planRunnerResult.getExecutionId();
        Execution execution = engine.getExecutionEngineContext().getExecutionAccessor().get(executionId);
        boolean tsFlag = (boolean) execution.getCustomField(TIMESERIES_FLAG);
        Assert.assertTrue(tsFlag);

        tsPlugin.serverStop(globalContext);

        TimeSeriesAggregationQuery keywordsQuery = new TimeSeriesAggregationQueryBuilder()
            .range(t1, t2)
            .withFilter(TimeSeriesFilterBuilder.buildFilter(Map.of("type", "keyword")))
            .withGroupDimensions(Set.of("name"))
            .build();
        TimeSeriesAggregationResponse keywordsBuckets = timeSeriesAggregationPipeline.collect(keywordsQuery);
        Assert.assertEquals(1, keywordsBuckets.getSeries().size());
        //assertEquals(50,keywordsBuckets.getSeries().values().stream().findFirst().get().values().stream().findFirst().get().getCount());
        TimeSeriesAggregationQuery customBucketsQuery = new TimeSeriesAggregationQueryBuilder()
            .range(t1, t2)
            .withFilter(TimeSeriesFilterBuilder.buildFilter(Map.of("type", "custom")))
            .withGroupDimensions(Set.of("name", "customAttr"))
            .build();
        TimeSeriesAggregationResponse customBuckets = timeSeriesAggregationPipeline.collect(customBucketsQuery);
        new TimeSeriesAggregationQueryBuilder()
            .range(t1, t2)
            .withFilter(TimeSeriesFilterBuilder.buildFilter(Map.of("type", "custom")))
            .withGroupDimensions(Set.of("name", "customAttr"))
            .build();
        if (timeSeriesAttributeMode.equals(Mode.NONE)) {
            //In this case we do not reduce the attributes to the included / excluded key, so we have 1 bucket per attributes set
            Assert.assertEquals(4, customBuckets.getSeries().size());
        } else {
            //customAttr is not part of the TS data as not part of the included attributes in the mode INCLUDED_ATTRIBUTES, or explicitly excluded in the mode EXCLUDED_ATTRIBUTES
            Assert.assertEquals(2, customBuckets.getSeries().size());
        }
    }

    @Keyword
    public void TestKeywordWithMeasurements() {
        output.addMeasure("myMeasure1", 1000, Map.of("customAttr", "val1"));
        output.addMeasure("myMeasure2", 1000, Map.of("customAttr", "val2"));
        output.addMeasure("myMeasure2", 100, Map.of("customAttr", "val3"));
        output.addMeasure("myMeasure2", 10, Map.of("customAttr", "val4"));
    }
}
