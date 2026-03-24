package step.plugins.livereporting;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.Unfiltered;
import step.core.metrics.CounterSnapshot;
import step.core.metrics.MetricSnapshot;
import step.core.metrics.MetricType;
import step.core.metrics.SampledSnapshot;
import step.core.reports.Measure;
import step.framework.server.security.NoSession;
import step.livereporting.LiveReportingContexts;

import java.io.IOException;
import java.util.List;

@Path("/live-reporting")
@Tag(name = "Live Reporting")
public class LiveReportingServices extends AbstractStepServices {

    private static final ObjectMapper SNAPSHOTS_MAPPER = createSnapshotsObjectMapper();

    private LiveReportingContexts liveReportingContexts;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        liveReportingContexts = globalContext.require(LiveReportingContexts.class);
    }

    @POST
    @Path("/{contextId}/measures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Unfiltered
    @NoSession
    public void injectMeasures(List<Measure> measures, @PathParam("contextId") String contextId) {
        liveReportingContexts.onMeasuresReceived(contextId, measures);
    }

    @POST
    @Path("/{contextId}/metrics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Unfiltered
    @NoSession
    public void injectMetrics(String body, @PathParam("contextId") String contextId) {
        try {
            List<MetricSnapshot> snapshots = SNAPSHOTS_MAPPER.readValue(body,
                SNAPSHOTS_MAPPER.getTypeFactory().constructCollectionType(List.class, MetricSnapshot.class));
            liveReportingContexts.onMetricsReceived(contextId, snapshots);
        } catch (IOException e) {
            throw new WebApplicationException("Failed to parse metrics payload: " + e.getMessage(),
                jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        }
    }

    private static ObjectMapper createSnapshotsObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MetricSnapshot.class, new MetricSnapshotDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    private static class
    MetricSnapshotDeserializer extends StdDeserializer<MetricSnapshot> {
        MetricSnapshotDeserializer() {
            super(MetricSnapshot.class);
        }

        @Override
        public MetricSnapshot deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode tree = p.readValueAsTree();
            JsonNode typeNode = tree.get("type");
            if (typeNode == null || typeNode.isNull()) {
                return null;
            }
            MetricType metricType;
            try {
                metricType = MetricType.valueOf(typeNode.asText());
            } catch (IllegalArgumentException e) {
                throw new IOException("Unknown metric type: " + typeNode.asText(), e);
            }
            switch (metricType) {
                case COUNTER:   return ctxt.readTreeAsValue(tree, CounterSnapshot.class);
                case GAUGE:
                case HISTOGRAM: return ctxt.readTreeAsValue(tree, SampledSnapshot.class);
                default: throw new IOException("Unhandled metric type: " + metricType);
            }
        }
    }
}
