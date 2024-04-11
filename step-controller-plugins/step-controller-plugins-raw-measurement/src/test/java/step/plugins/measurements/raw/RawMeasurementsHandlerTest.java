package step.plugins.measurements.raw;

import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Sleep;
import step.core.AbstractContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.objectenricher.*;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;
import static step.planbuilder.BaseArtefacts.sequence;
import static step.planbuilder.BaseArtefacts.sleep;

public class RawMeasurementsHandlerTest {

    @Test
    public void testRawMeasurementsHandler(){
        InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
        InMemoryExecutionAccessor executionAccessor = new InMemoryExecutionAccessor();
        InMemoryExecutionTaskAccessor scheduleAccessor = new InMemoryExecutionTaskAccessor();
        Sleep sleep = sleep(100);
        sleep.setInstrumentNode(new DynamicValue<>(true));
        Plan plan = PlanBuilder.create().startBlock(sequence()).add(sleep).endBlock().build();
        plan.addAttribute(AbstractOrganizableObject.NAME, "plantTest");


        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
        parentContext.setExecutionAccessor(executionAccessor);
        parentContext.setPlanAccessor(planAccessor);
        MeasurementPlugin measurementPlugin = new MeasurementPlugin(GaugeCollectorRegistry.getInstance());
        MeasurementAccessor measurementAccessor = new MeasurementAccessor(new InMemoryCollection<>(null, "measurements", Document.class, new ConcurrentHashMap()));
        RawMeasurementsHandler handler = new RawMeasurementsHandler(measurementAccessor);
        MeasurementPlugin.registerMeasurementHandlers(handler);
        try (ExecutionEngine engine = ExecutionEngine.builder().withParentContext(parentContext).withPlugin(measurementPlugin)
                .withPlugin(new BaseArtefactPlugin())
                .withObjectHookRegistry(getObjectHookRegistry()).build()) {
            ExecutionParameters executionParameters = new ExecutionParameters(ExecutionMode.RUN, plan, null, null, "my test", null, null, true, null);
            PlanRunnerResult execute = engine.execute(executionParameters);

            Document document = measurementAccessor.find(Filters.empty()).findFirst().orElseThrow();
            assertNotNull(document);
            assertEquals(13, document.size());
            assertEquals("val1", document.get("attr1"));
            assertEquals("val2", document.get("attr2"));
            assertFalse(document.containsKey("plan"));
            assertFalse(document.containsKey("schedule"));
            assertFalse(document.containsKey("execution"));
            assertEquals(execute.getExecutionId(), document.get("eId"));
            assertEquals("Sleep", document.get("name"));
            assertEquals(MeasurementPlugin.TYPE_CUSTOM, document.get("type"));
            assertEquals("PASSED", document.get("rnStatus"));
            assertEquals(plan.getId().toHexString(), document.get("planId"));
            assertEquals("", document.get("taskId"));
        }

    }

    @Test
    public void testWithSchedule() {
        InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
        InMemoryExecutionAccessor executionAccessor = new InMemoryExecutionAccessor();
        InMemoryExecutionTaskAccessor scheduleAccessor = new InMemoryExecutionTaskAccessor();
        Sleep sleep = sleep(100);
        sleep.setInstrumentNode(new DynamicValue<>(true));
        Plan plan = PlanBuilder.create().startBlock(sequence()).add(sleep).endBlock().build();
        plan.addAttribute(AbstractOrganizableObject.NAME, "plantTest");
        planAccessor.save(plan);



        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
        parentContext.setExecutionAccessor(executionAccessor);
        parentContext.setPlanAccessor(planAccessor);
        MeasurementPlugin measurementPlugin = new MeasurementPlugin(GaugeCollectorRegistry.getInstance());
        MeasurementAccessor measurementAccessor = new MeasurementAccessor(new InMemoryCollection<>(null, "measurements", Document.class, new ConcurrentHashMap()));
        RawMeasurementsHandler handler = new RawMeasurementsHandler(measurementAccessor);
        MeasurementPlugin.registerMeasurementHandlers(handler);
        try (ExecutionEngine engine = ExecutionEngine.builder().withParentContext(parentContext).withPlugin(measurementPlugin)
                .withPlugin(new BaseArtefactPlugin())
                .withObjectHookRegistry(getObjectHookRegistry()).build()) {
            ExecutionParameters executionParameters = new ExecutionParameters(ExecutionMode.RUN, plan, null, null, "my test", null, null, true, null);
            String executionId = engine.initializeExecution(executionParameters);
            Execution execution = executionAccessor.get(executionId);
            ExecutiontTaskParameters taskParameters = new ExecutiontTaskParameters();
            taskParameters.addAttribute(AbstractOrganizableObject.NAME, "my schedule");
            execution.setExecutiontTaskParameters(taskParameters);
            executionAccessor.save(execution);
            PlanRunnerResult execute = engine.execute(executionId);

            Document document = measurementAccessor.find(Filters.empty()).findFirst().orElseThrow();
            assertNotNull(document);
            assertEquals(13, document.size());
            assertEquals("val1", document.get("attr1"));
            assertEquals("val2", document.get("attr2"));
            assertFalse(document.containsKey("plan"));
            assertFalse(document.containsKey("schedule"));
            assertFalse(document.containsKey("execution"));
            assertEquals(execute.getExecutionId(), document.get("eId"));
            assertEquals("Sleep", document.get("name"));
            assertEquals(MeasurementPlugin.TYPE_CUSTOM, document.get("type"));
            assertEquals("PASSED", document.get("rnStatus"));
            assertEquals(plan.getId().toHexString(), document.get("planId"));
            assertEquals(taskParameters.getId().toHexString(), document.get("taskId"));


            //handler.afterExecutionEnd(executionContext);
            //the cleanup is done async after 70 seconds (to bet sure the last values is scrapped)
            /*try {
                Thread.sleep(80000);
                Map<String, LabelsSet> labelsByExecEnded = handler.getLabelsByExec();
                Assert.assertEquals(0,labelsByExecEnded.keySet().size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
    }


    private ObjectHookRegistry getObjectHookRegistry() {
        ObjectHookRegistry objectHooks = new ObjectHookRegistry();
        objectHooks.add(new ObjectHook() {
            @Override
            public ObjectFilter getObjectFilter(AbstractContext abstractContext) {
                return null;
            }

            @Override
            public ObjectEnricher getObjectEnricher(AbstractContext abstractContext) {
                return new ObjectEnricher() {
                    @Override
                    public TreeMap<String, String> getAdditionalAttributes() {
                        TreeMap<String, String> treeMap = new TreeMap<>();
                        treeMap.put("attr1","val1");
                        treeMap.put("attr2","val2");
                        return treeMap;
                    }

                    @Override
                    public void accept(EnricheableObject enricheableObject) {
                        enricheableObject.addAttribute("attr1","val1");
                        enricheableObject.addAttribute("attr2","val2");
                    }
                };
            }

            @Override
            public void rebuildContext(AbstractContext abstractContext, EnricheableObject enricheableObject) throws Exception {

            }

            @Override
            public boolean isObjectAcceptableInContext(AbstractContext abstractContext, EnricheableObject enricheableObject) {
                return true;
            }
        });
        return objectHooks;
    }
}
