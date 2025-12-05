package step.core.artefacts.reports.aggregated;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.common.managedoperations.Operation;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactTypeCache;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNode;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeAccessor;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeCachedAccessor;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.threadmanager.ThreadManager;
import step.core.timeseries.Resolution;
import step.core.timeseries.TimeSeriesCollectionsSettings;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.core.artefacts.reports.aggregated.ReportNodeTimeSeries.*;

public class AggregatedReportViewBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AggregatedReportViewBuilder.class);
    public static final String EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE = "execution.report.aggregated-tree.resolve-single-instance";
    public static final String MERGED_GROUPS_LABEL = "ALL";

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final ResolvedPlanNodeCachedAccessor resolvedPlanNodeCachedAccessor;
    private final ReportNodeTimeSeries mainReportNodesTimeSeries;
    private final ReportNodeAccessor mainReportNodeAccessor;
    private final boolean defaultResolveSingleInstanceReport;
    private final ThreadManager threadManager;

    public AggregatedReportViewBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.resolvedPlanNodeCachedAccessor = new ResolvedPlanNodeCachedAccessor(executionEngineContext.require(ResolvedPlanNodeAccessor.class), executionAccessor.get(executionId));
        this.mainReportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        this.defaultResolveSingleInstanceReport =  executionEngineContext.getConfiguration().getPropertyAsBoolean(EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE, true);
        this.mainReportNodesTimeSeries = executionEngineContext.require(ReportNodeTimeSeries.class);
        this.threadManager = executionEngineContext.get(ThreadManager.class);
    }

    public AggregatedReportView buildAggregatedReportView() {
        return buildAggregatedReportView(new AggregatedReportViewRequest());
    }

    public AggregatedReportView buildAggregatedReportView(AggregatedReportViewRequest request) {
        Optional<AggregatedReport> aggregatedReport = Optional.ofNullable(buildAggregatedReport(request));
        return aggregatedReport.map(ar -> ar.aggregatedReportView).orElse(null);
    }


    public FlatAggregatedReport buildFlatAggregatedReport(AggregatedReportViewRequest request) {
        Optional<AggregatedReport> aggregatedReport = Optional.ofNullable(buildAggregatedReport(request));
        return aggregatedReport.map(ar -> new FlatAggregatedReport(flattenAndFilterRecursively(ar.aggregatedReportView, request)))
                .orElse(null);
    }

    private static List<FlatAggregatedReportView> flattenAndFilterRecursively(AggregatedReportView aggregatedReportView, AggregatedReportViewRequest request) {
        List<FlatAggregatedReportView> result = new ArrayList<>();
        if (aggregatedReportView != null) {
            if (shouldIncludeAggregatedReport(request, aggregatedReportView.artefact)) {
                result.add(new FlatAggregatedReportView(aggregatedReportView));
            }
            if (aggregatedReportView.children != null) {
                for (AggregatedReportView child : aggregatedReportView.children) {
                    result.addAll(flattenAndFilterRecursively(child, request));
                }
            }
        }
        return result;
    }

    public AggregatedReport buildAggregatedReport(AggregatedReportViewRequest request) {
        Objects.requireNonNull(request);
        Execution execution = executionAccessor.get(executionId);
        ExecutionStatus status = execution.getStatus();
        boolean isExecutionRunning = status.equals(ExecutionStatus.RUNNING);
        boolean isExecutionAborting = status.equals(ExecutionStatus.ABORTING) || status.equals(ExecutionStatus.FORCING_ABORT);
        //Make sure the resolved Plan is available
        ResolvedPlanNode rootResolvedPlanNode = Optional.ofNullable(execution.getResolvedPlanRootNodeId()).map(resolvedPlanNodeCachedAccessor::getFromUnderlyingAccessor).orElse(null);
        if (rootResolvedPlanNode == null) {
            //No resolved plan found for the requested execution
            return null;
        } else if (request.selectedReportNodeId == null) {
            // Full aggregated report requested
            // We now (SED-3882) also  wand to get the count for RUNNING artefacts which can only be retrieved from report nodes RAW data
            Map<String, Long> runningCountByArtefactHash = new HashMap<>();
            Map<String, List<Operation>> operationsByArtefactHash = new HashMap<>();
            if (isExecutionRunning || isExecutionAborting) {
                try (Stream<ReportNode> reportNodeStream = mainReportNodeAccessor.getRunningReportNodesByExecutionID(executionId, getFrom(request.range), getTo(request.range))) {
                    reportNodeStream.forEach(reportNode -> {
                        runningCountByArtefactHash.merge(reportNode.getArtefactHash(), 1L, Long::sum);
                        if (request.fetchCurrentOperations && threadManager != null) {
                            operationsByArtefactHash.computeIfAbsent(reportNode.getArtefactHash(), k -> new ArrayList<>()).addAll(threadManager.getCurrentOperationsByReportNodeId(reportNode.getId().toHexString()));
                        }
                    });
                }
            }
            // Generate complete aggregated report tree
            //First aggregate time series data for the given execution context grouped by artefactHash and status as wells as artefactHash and error message
            CompletableFuture<Map<String, Map<String, Bucket>>> countByHashAndStatusFuture =
                    CompletableFuture.supplyAsync(() ->
                            mainReportNodesTimeSeries.queryByExecutionIdAndGroupBy(executionId, request.range, ARTEFACT_HASH, STATUS)
                    );
            CompletableFuture<Map<String, Map<String, Bucket>>> countByHashAndErrorMessageFuture =
                    CompletableFuture.supplyAsync(() ->
                            mainReportNodesTimeSeries.queryByExecutionIdAndGroupBy(executionId, request.range, ARTEFACT_HASH, ERROR_MESSAGE)
                    );
            Map<String, Map<String, Bucket>> countByHashAndStatus = countByHashAndStatusFuture.join();
            Map<String, Map<String, Bucket>> countByHashAndErrorMessage = countByHashAndErrorMessageFuture.join();
            //Because the time series time range extends to the time series resolution we need to use the same range when querying the report nodes
            Range resolvedRange = getResolvedRange(request, countByHashAndStatus);
            return new AggregatedReport(recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, countByHashAndStatus, countByHashAndErrorMessage, mainReportNodeAccessor, null, runningCountByArtefactHash, operationsByArtefactHash, resolvedRange));
        } else {
            // a node is selected to generate a partial aggregated report
            try (ReportNodeTimeSeries partialReportNodesTimeSeries = getInMemoryReportNodeTimeSeries()) {
                InMemoryReportNodeAccessor inMemoryReportNodeAccessor = new InMemoryReportNodeAccessor();
                AggregatedReport aggregatedReport = new AggregatedReport();
                //We now (SED-3882) also  wand to get the count for RUNNING artefacts which can only be retrieved from report nodes RAW data
                // For partial tree we anyway re-ingest the nodes and can populate the structure when doing so
                Map<String, Long> runningCountByArtefactHash = new HashMap<>();
                Map<String, List<Operation>> operationsByArtefactHash = new HashMap<>();
                Set<String> reportArtefactHashSet = buildPartialReportNodeTimeSeries(aggregatedReport, request.selectedReportNodeId, partialReportNodesTimeSeries, inMemoryReportNodeAccessor, runningCountByArtefactHash, operationsByArtefactHash, request.fetchCurrentOperations);
                // Only pass the reportArtefactHashSet if aggregate view filtering is enabled
                reportArtefactHashSet = (request.filterResolvedPlanNodes) ? reportArtefactHashSet : null;
                //Aggregate time series data for the given execution reporting context grouped by artefactHash
                Map<String, Map<String, Bucket>> countByHashAndStatus = partialReportNodesTimeSeries.queryByExecutionIdAndGroupBy(executionId, request.range,  ARTEFACT_HASH, STATUS);
                Map<String, Map<String, Bucket>> countByHashAndErrorMessage = mainReportNodesTimeSeries.queryByExecutionIdAndGroupBy(executionId, request.range, ARTEFACT_HASH, ERROR_MESSAGE);
                //Because the time series time range extends to the time series resolution we need to use the same range when querying the report nodes
                Range resolvedRange = getResolvedRange(request, countByHashAndStatus);
                aggregatedReport.aggregatedReportView = recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, countByHashAndStatus, countByHashAndErrorMessage, inMemoryReportNodeAccessor, reportArtefactHashSet, runningCountByArtefactHash, operationsByArtefactHash, resolvedRange);
                return aggregatedReport;
            }
        }
    }

    private ReportNodeTimeSeries.Range getResolvedRange(AggregatedReportViewRequest request, Map<String, Map<String, Bucket>> countByHashAndStatus) {
        Bucket firstBucket = countByHashAndStatus.values().stream()
                .flatMap(innerMap -> innerMap.values().stream())
                .findFirst()
                .orElse(null);
        ReportNodeTimeSeries.Range result = request.range;
        if (firstBucket != null) {
            result = new ReportNodeTimeSeries.Range(firstBucket.getBegin(), firstBucket.getEnd());
        }
        return result;
    }

    private ReportNodeTimeSeries getInMemoryReportNodeTimeSeries() {
        //Need to create a configuration with all time series details
        return new ReportNodeTimeSeries(new InMemoryCollectionFactory(new Properties()),
                // to build the report we only need a single time bucket and can flush only once all reports are ingested
                TimeSeriesCollectionsSettings.buildSingleResolutionSettings(Resolution.ONE_WEEK, 0), true);
    }

    /**
     * Populate an in memory timeseries and report node accessor with all data required to build a partial aggregated report tree
     * This aggregated tree will be filtered for the execution path of this single report node. If available we filter on the wrapping (nested) iteration
     * or simply on the selected node and its descendant
     *
     * @param aggregatedReport           the aggregatedReport, it will be populated with the resolved path
     * @param selectedReportNodeIdStr    the selected report node if for which we generate the partial report
     * @param reportNodeTimeSeries       the inMemory report node time series to be populated
     * @param reportNodeAccessor         the inMemory report node accessor to be populated
     * @param runningCountByArtefactHash the distribution of running artefacts by artefact hash
     * @param operationsByArtefactHash   the list of current operations for the artefact hash and its children
     * @return the set of artefact hash part of the partial report
     */
    private Set<String> buildPartialReportNodeTimeSeries(AggregatedReport aggregatedReport, String selectedReportNodeIdStr, ReportNodeTimeSeries reportNodeTimeSeries, ReportNodeAccessor reportNodeAccessor, Map<String, Long> runningCountByArtefactHash, Map<String, List<Operation>> operationsByArtefactHash, boolean fetchCurrentOperations) {
        ObjectId selectedReportNodeId = new ObjectId(selectedReportNodeIdStr);
        List<ReportNode> path = mainReportNodeAccessor.getReportNodePath(selectedReportNodeId);
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("Unable to determine the path of the selected node.");
        }
        Collections.reverse(path);
        ReportNode selectedReportNode = path.get(0);
        if (!selectedReportNode.getId().equals(selectedReportNodeId)) {
            throw new RuntimeException("Unable to determine the path of the selected node.");
        }
        // During ingestion, we store single report node per artefact hash in memory rather than saving all report nodes
        // There are only used to resolve single nodes when building the aggregated tree
        Map<String, ReportNode> singleReportNodes = new HashMap<>();
        // select the closest iteration node if any otherwise we fall back to the selected node
        ReportNode partialTreeRoot = path.stream().filter(this::isIterationNodeReport).findFirst().orElse(selectedReportNode);
        aggregatedReport.resolvedPartialPath = partialTreeRoot.getPath();
        ingestReportNodeRecursively(partialTreeRoot, reportNodeTimeSeries, singleReportNodes, runningCountByArtefactHash, operationsByArtefactHash, fetchCurrentOperations);
        reportNodeAccessor.save(singleReportNodes.values());
        reportNodeTimeSeries.flush();
        // build the set of artefact hash to be included in the report (re-ingested nodes + nodes of the path)
        Set<String> reportArtefactHashSet = new HashSet<>(singleReportNodes.keySet());
        reportArtefactHashSet.addAll(path.stream().map(ReportNode::getArtefactHash).collect(Collectors.toSet()));
        return reportArtefactHashSet;
    }

    private boolean isIterationNodeReport(ReportNode n) {
        AbstractArtefact resolvedArtefact = n.getResolvedArtefact();
        //All iteration work artefact are created with sequence artefact and names starting with "Iteration", we might change to a more robust implementation in the future
        return resolvedArtefact != null && resolvedArtefact.isWorkArtefact() && resolvedArtefact.getAttribute(AbstractOrganizableObject.NAME).startsWith("Iteration");
    }

    private void ingestReportNodeRecursively(ReportNode reportNode, ReportNodeTimeSeries reportNodeTimeSeries, Map<String, ReportNode> singleReportNodes, Map<String, Long> runningCountByArtefactHash, Map<String, List<Operation>> operationsByArtefactHash,  boolean fetchCurrentOperations) {
        if (reportNode.getStatus().equals(ReportNodeStatus.RUNNING)) {
            runningCountByArtefactHash.merge(reportNode.getArtefactHash(), 1L, Long::sum);
            if (fetchCurrentOperations && threadManager != null) {
                operationsByArtefactHash.computeIfAbsent(reportNode.getArtefactHash(), k -> new ArrayList<>()).addAll(threadManager.getCurrentOperationsByReportNodeId(reportNode.getId().toHexString()));
            }
        } else if (!reportNode.getStatus().equals(ReportNodeStatus.NORUN) && reportNode.getDuration() != null) {
            // When re-ingesting nodes to build up the partial aggregated tree, we only consider fully executed nodes as in ArtefactHandler.executes
            // While we could rely on report node status only, the strongest and safest requirement is that the report node duration must be set.
            reportNodeTimeSeries.ingestReportNode(reportNode);
        }
        singleReportNodes.put(reportNode.getArtefactHash(), reportNode);
        this.mainReportNodeAccessor.getChildren(reportNode.getId()).forEachRemaining(child -> ingestReportNodeRecursively(child, reportNodeTimeSeries, singleReportNodes, runningCountByArtefactHash, operationsByArtefactHash, fetchCurrentOperations));
    }

    /**
     * Create the aggregated report view for provided resolved plan node recursively populated
     *
     * @param resolvedPlanNode           the current resolved plan node for which we generate the aggregated report
     * @param request                    the aggregated report request
     * @param bucketByHashAndStatus      the report node time series data for given execution grouped by artefact hash and status
     * @param reportNodeAccessor         the report node access from which to fetch RAW data (i.e. for single report occurrences
     * @param filteredArtefactHashSet    the optional set of artefactHash to be included in the report (used for partial report tree)
     * @param runningCountByArtefactHash the inMemory count of running nodes by artefactHash used to enrich data from time-series
     * @param operationsByArtefactHash   the current operation for running nodes grouped by artefactHash
     * @return an aggregated report view for the provided resolved plan node
     */
    private AggregatedReportView recursivelyBuildAggregatedReportTree(ResolvedPlanNode resolvedPlanNode, AggregatedReportViewRequest request,
                                                                      Map<String, Map<String, Bucket>> bucketByHashAndStatus, Map<String, Map<String, Bucket>> countByHashAndErrorMessage,
                                                                      ReportNodeAccessor reportNodeAccessor, Set<String> filteredArtefactHashSet,
                                                                      Map<String, Long> runningCountByArtefactHash, Map<String, List<Operation>> operationsByArtefactHash,
                                                                      Range resolvedTimeserieRange) {
        String artefactHash = resolvedPlanNode.artefactHash;
        Map<String, Long> countByChildrenErrorMessage = new HashMap<>();
        List<AggregatedReportView> children = resolvedPlanNodeCachedAccessor.getByParentId(resolvedPlanNode.getId().toString())
                //filter nodes if filteredArtefactHashSet is provided and node is part of the set
                .filter(n -> filteredArtefactHashSet == null || filteredArtefactHashSet.contains(n.artefactHash))
                .map(n -> recursivelyBuildAggregatedReportTree(n, request, bucketByHashAndStatus, countByHashAndErrorMessage,
                        reportNodeAccessor, filteredArtefactHashSet, runningCountByArtefactHash, operationsByArtefactHash, resolvedTimeserieRange))
                //update the countByChildrenErrorMessage map before filtering children by artefact class
                .peek(c -> {
                    if (c.countByErrorMessage != null) {
                        c.countByErrorMessage.forEach((key, value) -> countByChildrenErrorMessage.merge(key, value, Long::sum));
                    }
                    if (c.countByChildrenErrorMessage != null) {
                        c.countByChildrenErrorMessage.forEach((key, value) -> countByChildrenErrorMessage.merge(key, value, Long::sum));
                    }
                })
                //if filtering on artefact classes is defined, we only include the artefact for the provided classes, otherwise we directly add all its children if any
                .flatMap(c -> shouldIncludeAggregatedReport(request, c.artefact) ? Stream.of(c) : c.children.stream())
                .collect(Collectors.toList());
        Map<String, Long> countByStatus = null;
        Map<String, Long> countByErrorMessage = null;
        Map<String, Bucket> bucketsByStatus = null;
        ReportNode singleInstanceReportNode = null;
        //Get the count by error message for this artefact in all cases allow to pull the errors to parent
        Map<String, Bucket> bucketByErrorMessage = countByHashAndErrorMessage.getOrDefault(artefactHash, new HashMap<>());
        countByErrorMessage = bucketByErrorMessage.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCount()));
        //Skip generation of statistics for reports that are not included based on request filter, root node is always IN
        if (resolvedPlanNode.parentId == null || shouldIncludeAggregatedReport(request, resolvedPlanNode.artefact)) {
            bucketsByStatus = bucketByHashAndStatus.getOrDefault(artefactHash, new HashMap<>());
            countByStatus = bucketsByStatus.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCount()));
            //enrich the count by status with running count, the buckets are not impacted
            Long runningCount = runningCountByArtefactHash.get(artefactHash);
            if (runningCount != null) {
                countByStatus.put(ReportNodeStatus.RUNNING.name(), runningCount);
            }
            //Create a "ALL" bucket merging the count by statuses
            if (!bucketsByStatus.isEmpty()) {
                Bucket bucket = bucketsByStatus.values().stream().findFirst().get();
                BucketBuilder bucketBuilder = new BucketBuilder(bucket.getBegin(), bucket.getEnd());
                bucketsByStatus.values().forEach(bucketBuilder::accumulate);
                bucketsByStatus.put(MERGED_GROUPS_LABEL, bucketBuilder.build());
            }
            //if the aggregated report is for a single instance we attach the RAW report if requested
            if (resolveSingleReport(request) && countByStatus.values().stream().reduce(0L, Long::sum) == 1) {
                singleInstanceReportNode = getSingleReportNodeInstance(reportNodeAccessor, executionId, artefactHash, resolvedTimeserieRange);
            }
        }
        boolean hasDescendantInvocations = hasDescendantInvocations(children);
        return new AggregatedReportView(resolvedPlanNode.artefact, artefactHash, countByStatus, countByErrorMessage, countByChildrenErrorMessage,
                children, hasDescendantInvocations, resolvedPlanNode.parentSource, singleInstanceReportNode, bucketsByStatus,
                operationsByArtefactHash.getOrDefault(artefactHash, new ArrayList<>()));
    }

    private boolean hasDescendantInvocations(List<AggregatedReportView> children) {
        return children.stream().anyMatch(c -> (c.hasDescendantInvocations || c.countByStatus.values().stream().anyMatch(l -> l > 0)));
    }

    private static boolean shouldIncludeAggregatedReport(AggregatedReportViewRequest request, AbstractArtefact artefact) {
        return request.filterArtefactClasses.isEmpty() || request.filterArtefactClasses.contains(ArtefactTypeCache.getArtefactName(artefact.getClass()));

    }

    private boolean resolveSingleReport(AggregatedReportViewRequest request) {
        return (request.resolveSingleInstanceReport != null) ? request.resolveSingleInstanceReport : defaultResolveSingleInstanceReport;
    }

    private Long getFrom(Range range) {
        return (range != null) ? range.from : null;
    }

    private Long getTo(Range range) {
        return (range != null) ? range.to : null;
    }

    private ReportNode getSingleReportNodeInstance(ReportNodeAccessor reportNodeAccessor, String executionId, String artefactHash, Range range) {
        Long from = getFrom(range);
        Long to = getTo(range);
        try (Stream<ReportNode> reportNodesByArtefactHash = reportNodeAccessor.getReportNodesByArtefactHash(executionId, artefactHash, from, to, 0, 2)) {
            List<ReportNode> reports = reportNodesByArtefactHash.collect(Collectors.toList());
            if (reports.size() == 1) {
                return reports.get(0);
            } else {
                String reportCountMsg = (reports.isEmpty()) ? "no" : "more than one";
                logger.error("Unexpected number of report nodes found. Found {} report for execution id {}, artefactHash: {} and range: {} to {}",
                        reportCountMsg, executionId, artefactHash, from, to);
                return null;
            }
        }
    }

}
