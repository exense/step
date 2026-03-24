package step.functions.handler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.metrics.CounterSnapshot;
import step.core.metrics.MetricSnapshot;
import step.core.metrics.MetricType;
import step.core.metrics.SampledSnapshot;

import java.io.IOException;

/**
 * Custom deserializer for the {@link MetricSnapshot} polymorphic hierarchy.
 * <p>
 * {@link MetricSnapshot} subclasses carry no {@code @JsonTypeInfo} annotations to keep
 * {@code step-api-reporting} free of Jackson dependencies. Instead, each subclass
 * exposes a plain {@link MetricSnapshot#getType()} bean property that is serialized as the
 * JSON field {@code "type"}. This deserializer reads that field and delegates to the
 * correct concrete class.
 */
class MetricSnapshotDeserializer extends StdDeserializer<MetricSnapshot> {

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
            case COUNTER:
                return ctxt.readTreeAsValue(tree, CounterSnapshot.class);
            case GAUGE:
            case HISTOGRAM:
                return ctxt.readTreeAsValue(tree, SampledSnapshot.class);
            default:
                throw new IOException("Unhandled metric type: " + metricType);
        }
    }
}
