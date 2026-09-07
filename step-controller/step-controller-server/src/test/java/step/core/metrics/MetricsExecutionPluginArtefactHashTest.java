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
package step.core.metrics;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.livereporting.LiveReportingPlugin;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Verifies that measurements and metrics carry the {@code artefactHash} of the report node they were
 * produced by, i.e. the position of the artefact within the resolved plan. The same keyword called
 * from two different positions must yield two distinct hashes, both matching the hash of the
 * corresponding report node.
 */
public class MetricsExecutionPluginArtefactHashTest extends AbstractKeyword {

    private static final String KEYWORD_NAME = "TestKeywordForArtefactHash";

    private ExecutionEngine engine;
    private CapturingHandler capturingHandler;

    private static class CapturingHandler implements MetricSamplesHandler {

        final CopyOnWriteArrayList<Measurement> capturedMeasurements = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<ExecutionMetricSample> capturedMetrics = new CopyOnWriteArrayList<>();

        @Override
        public void processMeasurements(ExecutionContext executionContext, List<Measurement> measurements) {
            capturedMeasurements.addAll(measurements);
        }

        @Override
        public void processMetrics(ExecutionContext executionContext, List<ExecutionMetricSample> metrics) {
            capturedMetrics.addAll(metrics);
        }
    }

    @Before
    public void setUp() {
        MetricsControllerPlugin mc = new MetricsControllerPlugin();
        mc.initMetricSamplingAndHeartbeat(MetricsControllerPlugin.METRICS_SAMPLING_INTERVAL_SECONDS_DEFAULT);
        capturingHandler = new CapturingHandler();
        MetricsExecutionPlugin.registerSamplesHandlers(capturingHandler);
        engine = ExecutionEngine.builder()
            .withPlugin(new MetricsExecutionPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new LocalFunctionPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new LiveReportingPlugin())
            .build();
    }

    @Test
    public void measurementsAndMetricsCarryTheArtefactHashOfTheirReportNode() {
        // The same keyword at two different positions of the plan
        Plan plan = PlanBuilder.create()
            .startBlock(BaseArtefacts.sequence())
            .startBlock(FunctionArtefacts.keyword(KEYWORD_NAME))
            .endBlock()
            .startBlock(FunctionArtefacts.keyword(KEYWORD_NAME))
            .endBlock()
            .endBlock()
            .build();

        Set<String> keywordNodeHashes = new HashSet<>();
        engine.execute(plan).visitReportNodes(node -> {
            if (KEYWORD_NAME.equals(node.getName())) {
                keywordNodeHashes.add(node.getArtefactHash());
            }
        });
        Assert.assertEquals("The keyword is expected at two distinct positions of the plan", 2, keywordNodeHashes.size());
        Assert.assertFalse(keywordNodeHashes.contains(null));

        // Every measurement produced by the keyword calls carries the hash of its report node
        List<Measurement> keywordMeasurements = capturingHandler.capturedMeasurements.stream()
            .filter(m -> KEYWORD_NAME.equals(m.getName()))
            .collect(Collectors.toList());
        Assert.assertEquals(2, keywordMeasurements.size());
        Set<String> measurementHashes = keywordMeasurements.stream()
            .map(m -> (String) m.get(MetricsExecutionPlugin.ARTEFACT_HASH))
            .collect(Collectors.toSet());
        Assert.assertEquals(keywordNodeHashes, measurementHashes);

        // Same for the custom measures added by the keyword
        capturingHandler.capturedMeasurements.stream()
            .filter(m -> "myMeasure".equals(m.getName()))
            .forEach(m -> Assert.assertTrue("Unexpected artefact hash on measure " + m,
                keywordNodeHashes.contains(m.get(MetricsExecutionPlugin.ARTEFACT_HASH))));

        // And for the metrics emitted by the keyword, which must also expose it as an effective label
        List<ExecutionMetricSample> counters = capturingHandler.capturedMetrics.stream()
            .filter(mm -> "eventCount".equals(mm.sample.getName()))
            .collect(Collectors.toList());
        Assert.assertEquals(2, counters.size());
        Set<String> metricHashes = counters.stream().map(mm -> mm.artefactHash).collect(Collectors.toSet());
        Assert.assertEquals(keywordNodeHashes, metricHashes);
        counters.forEach(mm -> Assert.assertEquals(mm.artefactHash,
            mm.getEffectiveLabels().get(MetricsExecutionPlugin.ARTEFACT_HASH)));
    }

    @Keyword
    public void TestKeywordForArtefactHash() {
        output.addMeasure("myMeasure", 100);
        output.newCounter("eventCount").increment(1);
    }
}
