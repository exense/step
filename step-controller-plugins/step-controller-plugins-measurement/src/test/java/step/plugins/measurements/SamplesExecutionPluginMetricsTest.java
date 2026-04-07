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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.core.GlobalContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.metrics.*;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.livereporting.LiveReportingPlugin;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tests that running a keyword that adds output metrics via
 * {@code output.addCounter/addGauge/addHistogram} routes those metrics through
 * {@link SamplesHandler#processMetrics} with correctly enriched
 * {@link StepMetricSample} objects.
 * <p>
 * This class is intentionally separate from {@link SamplesExecutionPluginTest} to avoid
 * the static {@code measurementHandlers} list accumulating handlers across tests.
 */
public class SamplesExecutionPluginMetricsTest extends AbstractKeyword {

    private ExecutionEngine engine;

    /**
     * Captures all {@link StepMetricSample}s delivered to {@link #processMetrics}.
     * No-op {@link #processMeasurements} to avoid side effects on shared state.
     */
    private static class CapturingSamplesHandler implements SamplesHandler {

        final CopyOnWriteArrayList<StepMetricSample> capturedMetrics = new CopyOnWriteArrayList<>();

        @Override
        public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        }

        @Override
        public void processMeasurements(List<Measurement> measurements) {
            // no-op — not under test here
        }

        @Override
        public void processGauges(List<Measurement> measurements) {
            // no-op
        }

        @Override
        public void processMetrics(List<StepMetricSample> metrics) {
            capturedMetrics.addAll(metrics);
        }

        @Override
        public void afterExecutionEnd(ExecutionContext context) {
        }
    }

    private CapturingSamplesHandler capturingHandler;

    @Before
    public void setUp() throws Exception {
        SamplesControllerPlugin mc = new SamplesControllerPlugin();
        mc.initGaugeCollectorRegistry(GlobalContextBuilder.createGlobalContext());
        capturingHandler = new CapturingSamplesHandler();
        SamplesExecutionPlugin.registerSamplesHandlers(capturingHandler);
        engine = ExecutionEngine.builder()
            .withPlugin(new SamplesExecutionPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new LocalFunctionPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new LiveReportingPlugin())
            .build();
    }

    @Test
    public void testOutputMetricsTriggerProcessMetrics() {
        Plan plan = PlanBuilder.create()
            .startBlock(FunctionArtefacts.keyword("TestKeywordWithMetrics"))
            .endBlock()
            .build();

        engine.execute(plan);

        List<StepMetricSample> metrics = capturingHandler.capturedMetrics;

        // Expect exactly 3 metric measurements: counter, gauge, histogram
        Assert.assertEquals(3, metrics.size());

        // Counter
        StepMetricSample counterMm = findByName(metrics, "eventCount");
        Assert.assertNotNull("Counter metric 'eventCount' not found", counterMm);
        Assert.assertEquals(InstrumentType.COUNTER, counterMm.sample.getType());
        MetricSample counter = (MetricSample) counterMm.sample;
        Assert.assertEquals(5, counter.getCount());
        Assert.assertEquals(5, counter.getLast());

        // Gauge
        StepMetricSample gaugeMm = findByName(metrics, "queueDepth");
        Assert.assertNotNull("Gauge metric 'queueDepth' not found", gaugeMm);
        Assert.assertEquals(InstrumentType.GAUGE, gaugeMm.sample.getType());
        MetricSample gauge = gaugeMm.sample;
        Assert.assertEquals(2, gauge.getCount());
        Assert.assertEquals(57, gauge.getSum()); // 42 + 15
        Assert.assertEquals(15, gauge.getMin());
        Assert.assertEquals(42, gauge.getMax());

        // Histogram
        StepMetricSample histMm = findByName(metrics, "responseTimeMs");
        Assert.assertNotNull("Histogram metric 'responseTimeMs' not found", histMm);
        Assert.assertEquals(InstrumentType.HISTOGRAM, histMm.sample.getType());
        MetricSample hist = (MetricSample) histMm.sample;
        Assert.assertEquals(2, hist.getCount());
        Assert.assertEquals(350, hist.getSum()); // 100 + 250

        // Effective labels must include the execution ID
        String execId = counterMm.eId;
        Assert.assertNotNull(execId);
        Assert.assertEquals(execId, counterMm.getEffectiveLabels().get(SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID));
    }

    private StepMetricSample findByName(List<StepMetricSample> metrics, String name) {
        return metrics.stream()
            .filter(mm -> name.equals(mm.sample.getName()))
            .findFirst()
            .orElse(null);
    }

    @Keyword
    public void TestKeywordWithMetrics() {
        output.addCounter("eventCount").increment(5);

        GaugeMetric gauge = output.addGauge("queueDepth");
        gauge.observe(42);
        gauge.observe(15);

        HistogramMetric hist = output.addHistogram("responseTimeMs");
        hist.observe(100);
        hist.observe(250);
    }
}
