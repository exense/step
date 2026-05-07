package step.core.execution.table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.reports.ReportNode;

import java.io.IOException;
import java.util.List;

/**
 * A server-side wrapper that enriches any {@link ReportNode} subclass with additional
 * fields without losing the delegate's concrete type on the wire.
 * <p>
 * Because Java's single-inheritance model prevents {@code class Enriched<T> extends T},
 * this class uses <em>composition</em>: the delegate node is held as a typed field and
 * the custom {@link Serializer} forwards its serialized form (including the original
 * {@code _class} discriminator) before appending the enrichment fields.  The result
 * seen by the client is indistinguishable from the delegate's own JSON, extended with
 * the extra fields.
 * <p>
 * This class extends {@link ReportNode} only so that it fits the {@code Table<ReportNode>}
 * type parameter used by the table infrastructure; its own fields are never serialized.
 *
 * @param <T> the concrete {@link ReportNode} subtype being enriched
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)   // suppress inherited _class — the serializer emits the delegate's _class instead
@JsonSerialize(using = EnrichedReportNode.Serializer.class)
public class EnrichedReportNode<T extends ReportNode> extends ReportNode {

    @JsonIgnore
    private final T delegate;

    private final List<ReportNode> assertionReportNodesOnError;

    public EnrichedReportNode(T delegate, List<ReportNode> assertionReportNodesOnError) {
        this.delegate = delegate;
        this.assertionReportNodesOnError = assertionReportNodesOnError;
    }

    /**
     * Returns the original report node being enriched.
     */
    public T getDelegate() {
        return delegate;
    }

    /**
     * Returns the assertion report nodes collected from failed children of the delegate,
     * or {@code null} when no enrichment was performed.
     */
    public List<ReportNode> getAssertionReportNodesOnError() {
        return assertionReportNodesOnError;
    }

    /**
     * Merges the delegate's serialized JSON (preserving its {@code _class} discriminator
     * and all subclass fields) with the enrichment fields of this wrapper.
     */
    public static class Serializer extends JsonSerializer<EnrichedReportNode<?>> {

        @Override
        public void serialize(EnrichedReportNode<?> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
            ObjectMapper mapper = (ObjectMapper) gen.getCodec();
            // valueToTree honours @JsonTypeInfo — the delegate's _class is included
            ObjectNode node = mapper.valueToTree(value.delegate);
            if (value.assertionReportNodesOnError != null) {
                ArrayNode arrayNode = mapper.createArrayNode();
                for (ReportNode assertionNode : value.assertionReportNodesOnError) {
                    arrayNode.add(mapper.valueToTree(assertionNode));
                }
                node.set("assertionReportNodesOnError", arrayNode);
            }
            mapper.writeTree(gen, node);
        }

        /**
         * Called by Jackson when the element type in a polymorphic container triggers type-info
         * wrapping.  We bypass the supplied {@link TypeSerializer} entirely: our {@link #serialize}
         * already writes the correct {@code _class} via the delegate's own serialization, so adding
         * another type-info layer would corrupt the output.
         */
        @Override
        public void serializeWithType(EnrichedReportNode<?> value, JsonGenerator gen,
                                      SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            serialize(value, gen, serializers);
        }
    }
}
