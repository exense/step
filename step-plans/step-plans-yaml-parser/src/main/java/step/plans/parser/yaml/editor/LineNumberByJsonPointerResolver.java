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
package step.plans.parser.yaml.editor;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LineNumberByJsonPointerResolver {

    private static final Pattern SPECIAL_CHARS = Pattern.compile("[\\s~!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+");

    private final CustomJsonNodeFactory jsonNodeFactory;
    private final Configuration jsonConfig;

    public LineNumberByJsonPointerResolver() {
        CustomParserFactory customParserFactory = new CustomParserFactory();
        ObjectMapper om = new ObjectMapper(customParserFactory);

        jsonNodeFactory = new CustomJsonNodeFactory(om.getDeserializationConfig().getNodeFactory(), customParserFactory);
        om.setConfig(om.getDeserializationConfig().with(jsonNodeFactory));
        jsonConfig = Configuration.builder()
                .mappingProvider(new JacksonMappingProvider(om))
                .jsonProvider(new JacksonJsonNodeJsonProvider(om))
                .options(Option.ALWAYS_RETURN_LIST)
                .build();
    }

    /**
     * Maps the jsonPointers to their line numbers in original source file
     *
     * @param jsonPointers the json pointers to be resolved to their line numbers in source
     * @param source       the source (yaml)
     * @return the mapping between json pointer and source line for each element from jsonPointers
     */
    public List<JsonPointerSourceLine> findLineNumbers(List<String> jsonPointers, String source) {
        return findLineNumbersInternal(jsonPointers, source, false).getSourceLines();
    }

    /**
     * Returns the list of parsed json nodes from source (used for test purposes)
     */
    protected synchronized LineNumberResolveInternalResult findLineNumbersInternal(List<String> jsonPointers, String source, boolean returnJsonNodeAllocation) {
        try {
            jsonNodeFactory.getLocationMapping().clear();

            List<JsonPointerSourceLine> result = new ArrayList<>();
            DocumentContext parsedDocument = JsonPath.parse(source, jsonConfig);

            for (String jsonPointer : jsonPointers) {
                // reference to the whole json
                if (jsonPointer.equals("#")) {
                    result.add(new JsonPointerSourceLine(jsonPointer, 1));
                    continue;
                }

                ArrayNode findings = parsedDocument.read(JsonPath.compile(jsonPointerToJsonPath(jsonPointer)));
                if (findings.isEmpty()) {
                    result.add(new JsonPointerSourceLine(jsonPointer, 1));
                } else {
                    for (JsonNode finding : findings) {
                        // use collected location mappings
                        JsonLocation location = jsonNodeFactory.getLocationForNode(finding);
                        result.add(new JsonPointerSourceLine(jsonPointer, location.getLineNr()));
                    }
                }
            }
            return new LineNumberResolveInternalResult(result, returnJsonNodeAllocation ? new ArrayList<>(jsonNodeFactory.getLocationMapping()) : null);
        } finally {
            jsonNodeFactory.getLocationMapping().clear();
        }
    }

    protected static class LineNumberResolveInternalResult {
        private final List<JsonPointerSourceLine> sourceLines;
        private final List<Map.Entry<JsonNode, JsonLocation>> allocationMapping;

        public LineNumberResolveInternalResult(List<JsonPointerSourceLine> sourceLines, List<Map.Entry<JsonNode, JsonLocation>> allocationMapping) {
            this.sourceLines = sourceLines;
            this.allocationMapping = allocationMapping;
        }

        public List<JsonPointerSourceLine> getSourceLines() {
            return sourceLines;
        }

        public List<Map.Entry<JsonNode, JsonLocation>> getAllocationMapping() {
            return allocationMapping;
        }
    }

    public static class JsonPointerSourceLine {
        private final String jsonPointer;
        private final int sourceLine;

        public JsonPointerSourceLine(String jsonPointer, int sourceLine) {
            this.jsonPointer = jsonPointer;
            this.sourceLine = sourceLine;
        }

        public String getJsonPointer() {
            return jsonPointer;
        }

        public int getSourceLine() {
            return sourceLine;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            JsonPointerSourceLine that = (JsonPointerSourceLine) object;
            return sourceLine == that.sourceLine && Objects.equals(jsonPointer, that.jsonPointer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jsonPointer, sourceLine);
        }
    }

    public String jsonPointerToJsonPath(String jsonPointer) {
        if (jsonPointer.isEmpty()) {
            return "$";
        }

        if (jsonPointer.equals("/")) {
            return "$['']";
        }

        List<String> tokens = parse(jsonPointer);
        StringBuilder jsonPath = new StringBuilder("$");

        for (String token : tokens) {
            if (SPECIAL_CHARS.matcher(token).matches()) {
                jsonPath.append("[").append(token).append("]");
            } else {
                jsonPath.append(".").append(token);
            }
        }


        return jsonPath.toString();
    }

    private List<String> parse(String jsonPointer) {
        if (jsonPointer.startsWith("#")) {
            jsonPointer = jsonPointer.substring(1);
        }
        if (jsonPointer.charAt(0) != '/') {
            throw new RuntimeException("Invalid JSON pointer:" + jsonPointer);
        }
        return Arrays.stream(jsonPointer.substring(1).split("/")).collect(Collectors.toList());
    }

    private static class CustomParserFactory extends YAMLFactory {

        private static final long serialVersionUID = -7523974986510864179L;
        private YAMLParser parser;

        public YAMLParser getParser() {
            return this.parser;
        }

        @Override
        public YAMLParser createParser(Reader r) throws IOException, JsonParseException {
            parser = super.createParser(r);
            return parser;
        }

        @Override
        public YAMLParser createParser(String content) throws IOException, JsonParseException {
            parser = super.createParser(content);
            return parser;
        }
    }

    private static class CustomJsonNodeFactory extends JsonNodeFactory {

        private static final long serialVersionUID = 8807395553661461181L;

        private final JsonNodeFactory delegate;
        private final CustomParserFactory parserFactory;

        /*
         * "Why isn't this a map?" you might be wondering. Well, when the nodes are created, they're all
         * empty and a node's hashCode is based on its children. So if you use a map and put the node
         * in, then the node's hashCode is based on no children, then when you lookup your node, it is
         * *with* children, so the hashcodes are different. Instead of all of this, you have to iterate
         * through a listing and find their matches once the objects have been populated, which is only
         * after the document has been completely parsed
         */
        private final List<Map.Entry<JsonNode, JsonLocation>> locationMapping;

        public CustomJsonNodeFactory(JsonNodeFactory nodeFactory,
                                     CustomParserFactory parserFactory) {
            delegate = nodeFactory;
            this.parserFactory = parserFactory;
            locationMapping = Collections.synchronizedList(new ArrayList<>());
        }

        /**
         * Given a node, find its location, or null if it wasn't found
         *
         * @param jsonNode the node to search for
         * @return the location of the node or null if not found
         */
        public JsonLocation getLocationForNode(JsonNode jsonNode) {
            return this.locationMapping.stream().filter(e -> e.getKey().equals(jsonNode))
                    .map(Map.Entry::getValue).findAny().orElse(null);
        }

        /**
         * Simple interceptor to mark the node in the lookup list and return it back
         *
         * @param <T>  the type of the JsonNode
         * @param node the node itself
         * @return the node itself, having marked its location
         */
        private <T extends JsonNode> T markNode(T node) {
            JsonLocation loc = parserFactory.getParser().getCurrentLocation();
            locationMapping.add(new AbstractMap.SimpleEntry<>(node, loc));
            return node;
        }

        @Override
        public BooleanNode booleanNode(boolean v) {
            return markNode(delegate.booleanNode(v));
        }

        @Override
        public NullNode nullNode() {
            return markNode(delegate.nullNode());
        }

        @Override
        public NumericNode numberNode(byte v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Byte value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public NumericNode numberNode(short v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Short value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public NumericNode numberNode(int v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Integer value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public NumericNode numberNode(long v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Long value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public ValueNode numberNode(BigInteger v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public NumericNode numberNode(float v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Float value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public NumericNode numberNode(double v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public ValueNode numberNode(Double value) {
            return markNode(delegate.numberNode(value));
        }

        @Override
        public ValueNode numberNode(BigDecimal v) {
            return markNode(delegate.numberNode(v));
        }

        @Override
        public TextNode textNode(String text) {
            return markNode(delegate.textNode(text));
        }

        @Override
        public ObjectNode objectNode() {
            return markNode(delegate.objectNode());
        }

        @Override
        public ArrayNode arrayNode() {
            return markNode(delegate.arrayNode());
        }

        @Override
        public ArrayNode arrayNode(int capacity) {
            return markNode(delegate.arrayNode(capacity));
        }

        protected List<Map.Entry<JsonNode, JsonLocation>> getLocationMapping() {
            return locationMapping;
        }
    }
}
