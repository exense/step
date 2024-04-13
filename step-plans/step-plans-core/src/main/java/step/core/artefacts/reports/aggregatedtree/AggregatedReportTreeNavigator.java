package step.core.artefacts.reports.aggregatedtree;

import step.core.AbstractContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.handlers.ArtefactHashGenerator;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class AggregatedReportTreeNavigator {

    private final ExecutionEngineContext executionEngineContext;
    private final ExecutionAccessor executionAccessor;
    private final PlanAccessor planAccessor;
    private final ReportNodeAccessor reportNodeAccessor;
    private final ArtefactHashGenerator artefactHashGenerator;
    private final FunctionAccessor functionAccessor;
    private final DynamicJsonObjectResolver dynamicJsonObjectResolver;
    private final ArtefactHandlerRegistry artefactHandlerRegistry;
    private final ObjectHookRegistry objectHookRegistry;

    public AggregatedReportTreeNavigator(ExecutionEngineContext executionEngineContext) {
        this.executionEngineContext = executionEngineContext;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.planAccessor = executionEngineContext.getPlanAccessor();
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        functionAccessor = executionEngineContext.require(FunctionAccessor.class);
        artefactHandlerRegistry = executionEngineContext.getArtefactHandlerRegistry();
        dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(executionEngineContext.getExpressionHandler()));
        artefactHashGenerator = new ArtefactHashGenerator();
        objectHookRegistry = executionEngineContext.require(ObjectHookRegistry.class);
    }

    public static class Node {

        public final AbstractArtefact artefact;
        public final String artefactHash;
        public final long callCount;
        public final List<Node> children;

        public Node(AbstractArtefact artefact, String artefactHash, long callCount, List<Node> children) {
            this.artefact = artefact;
            this.artefactHash = artefactHash;
            this.callCount = callCount;
            this.children = children;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            recursiveToString(this, 0, stringBuilder);
            return stringBuilder.toString();
        }

        private void recursiveToString(Node node, int level, StringBuilder stringBuilder) {
            String name = node.artefact.getClass().getSimpleName();
            String indentation = new String(new char[level]).replace("\0", " ");
            stringBuilder.append(indentation).append(name).append(": ").append(node.callCount).append("x\n");
            node.children.forEach(c -> recursiveToString(c, level + 1, stringBuilder));
        }
    }

    public Node getAggregatedReportTree(String executionId) {
        Execution execution = Objects.requireNonNull(executionAccessor.get(executionId));
        ObjectPredicate objectPredicate = getObjectPredicate(execution);
        String planId = execution.getPlanId();
        Plan plan = execution.getPlanSnapshots().stream().filter(p -> p.getId().toString().equals(planId)).findFirst().get();
        return buildTreeRecursively(executionId, plan.getRoot(), null, objectPredicate);
    }

    private ObjectPredicate getObjectPredicate(Execution execution) {
        AbstractContext context = new AbstractContext() {};
        try {
            objectHookRegistry.rebuildContext(context, execution);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return objectHookRegistry.getObjectPredicate(context);
    }

    public Stream<ReportNode> getNodesByArtefactHash(String executionId, String artefactHash) {
        return reportNodeAccessor.getReportNodesByArtefactHash(executionId, artefactHash);
    }

    private Node buildTreeRecursively(String executionId, AbstractArtefact artefactNode, String artefactPath, ObjectPredicate objectPredicate) {
        String artefactId = artefactNode.getId().toString();
        String artefactHash = artefactHashGenerator.generateArtefactHash(artefactPath, artefactId);
        long callCount = reportNodeAccessor.countReportNodesByArtefactHash(executionId, artefactHash);

        // Resolve children
        List<AbstractArtefact> childrenArtefacts;
        String newArtefactPath;
        if (artefactNode.isCallingArtefactsFromOtherPlans()) {
            ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerRegistry.getArtefactHandler((Class<AbstractArtefact>) artefactNode.getClass());
            AbstractArtefact referencedChildArtefact = artefactHandler.resolveArtefactCall(artefactNode, dynamicJsonObjectResolver, Map.of(), objectPredicate, planAccessor, functionAccessor);
            childrenArtefacts = List.of(referencedChildArtefact);
            newArtefactPath = ArtefactHashGenerator.getPath(artefactPath, artefactNode.getId().toString());
        } else {
            childrenArtefacts = artefactNode.getChildren();
            newArtefactPath = artefactPath;
        }

        // Recursively call children artefacts
        LinkedList<Node> childrenNodes = new LinkedList<>();
        for (AbstractArtefact child : childrenArtefacts) {
            Node childNode = buildTreeRecursively(executionId, child, newArtefactPath, objectPredicate);
            childrenNodes.add(childNode);
        }

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = executionEngineContext.getDynamicBeanResolver().cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        return new Node(artefactClone, artefactHash, callCount, childrenNodes);
    }
}
