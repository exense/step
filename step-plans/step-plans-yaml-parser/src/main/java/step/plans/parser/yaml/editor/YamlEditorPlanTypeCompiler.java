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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import org.everit.json.schema.ValidationException;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.plans.PlanCompilationError;
import step.core.plans.PlanCompiler;
import step.core.plans.PlanCompilerException;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class YamlEditorPlanTypeCompiler implements PlanCompiler<YamlEditorPlan> {

    private final YamlPlanReader reader;

    public YamlEditorPlanTypeCompiler() {
        // TODO: think if we need to configure the actual plan version here (now the default YamlPlanVersions.ACTUAL_VERSION is always used)
        // the reader has 'validateWithJsonSchema'=true
        reader = new YamlPlanReader(null, null, true, null);
    }

    @Override
    public YamlEditorPlan compile(YamlEditorPlan plan) throws PlanCompilerException {
        String source = plan.getSource();
        Plan parsedPlan;

        // TODO: source == null or empty?
        try (InputStream is = new ByteArrayInputStream(source.getBytes())) {
            parsedPlan = reader.readYamlPlan(is);
        } catch (YamlPlanValidationException e) {
            PlanCompilerException planCompilerException = new PlanCompilerException();
            if (e.getCause() != null && e.getCause() instanceof ValidationException) {
                ValidationException detailedValidationException = (ValidationException) e.getCause();
                for (ValidationException causingException : detailedValidationException.getCausingExceptions()) {
                    YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
                    compilationError.setMessage(causingException.getErrorMessage());
                    String pointerToViolation = detailedValidationException.getPointerToViolation();
                    if (pointerToViolation != null && !pointerToViolation.isEmpty()) {
                        compilationError.setLine(findLineNumber(JsonPointer.valueOf(pointerToViolation), source));
                    }
                    planCompilerException.addError(compilationError);
                }
            } else {
                YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
                compilationError.setMessage(e.getMessage());
                planCompilerException.addError(compilationError);
            }
            throw planCompilerException;
        } catch (Exception e) {
            PlanCompilerException planCompilerException = new PlanCompilerException();
            YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
            compilationError.setMessage(e.getMessage());
            planCompilerException.addError(compilationError);
            throw planCompilerException;
        }

        AbstractArtefact root = plan.getRoot();
        AbstractArtefact parsedRoot = parsedPlan.getRoot();

        // Make sure to keep the same id and attributes for root element
        if (root != null && root.getClass() == parsedRoot.getClass()) {
            parsedRoot.setId(root.getId());
            parsedRoot.setAttributes(root.getAttributes());
        }

        plan.setRoot(parsedRoot);
        plan.setSubPlans(parsedPlan.getSubPlans());
        plan.setFunctions(parsedPlan.getFunctions());

        return plan;
    }

    private int findLineNumber(JsonPointer pointer, String source) {
        // TODO: resolve source line by pointer

        /*
       CustomParserFactory customParserFactory = new CustomParserFactory();
        ObjectMapper om = new ObjectMapper(customParserFactory);
        CustomJsonNodeFactory factory = new CustomJsonNodeFactory(om.getDeserializationConfig().getNodeFactory(), customParserFactory);
        om.setConfig(om.getDeserializationConfig().with(factory));
        Configuration config = Configuration.builder()
                .mappingProvider(new JacksonMappingProvider(om))
                .jsonProvider(new JacksonJsonNodeJsonProvider(om))
                .options(Option.ALWAYS_RETURN_LIST)
                .build();

        File filePath = ...;
        JsonPath jsonPath = ...;
        DocumentContext parsedDocument = JsonPath.parse(filePath, config);
        ArrayNode findings = parsedDocument.read(jsonPath);
        for (JsonNode finding : findings) {
            JsonLocation location = factory.getLocationForNode(finding);
            int lineNum = location.getLineNr();
            //Do something with lineNum
        }
        */
        return 1;
    }

    static class YamlEditorPlanCompilationError extends PlanCompilationError {

        private int line;

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        @Override
        public String toString() {
            return "YamlEditorPlanCompilationError{" +
                    "message=" + getMessage() +
                    "line=" + line +
                    '}';
        }
    }

    private class CustomParserFactory extends JsonFactory {

        private static final long serialVersionUID = -7523974986510864179L;
        private JsonParser parser;

        public JsonParser getParser() {
            return this.parser;
        }

        @Override
        public JsonParser createParser(Reader r) throws IOException, JsonParseException {
            parser = super.createParser(r);
            return parser;
        }

        @Override
        public JsonParser createParser(String content) throws IOException, JsonParseException {
            parser = super.createParser(content);
            return parser;
        }
    }

    private class CustomJsonNodeFactory extends JsonNodeFactory {

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
        private List<Map.Entry<JsonNode, JsonLocation>> locationMapping;

        public CustomJsonNodeFactory(JsonNodeFactory nodeFactory,
                                     CustomParserFactory parserFactory) {
            delegate = nodeFactory;
            this.parserFactory = parserFactory;
            locationMapping = new ArrayList<>();
        }

        /**
         * Given a node, find its location, or null if it wasn't found
         *
         * @param jsonNode the node to search for
         * @return the location of the node or null if not found
         */
        public JsonLocation getLocationForNode(JsonNode jsonNode) {
            return this.locationMapping.stream().filter(e -> e.getKey().equals(jsonNode))
                    .map(e -> e.getValue()).findAny().orElse(null);
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

    }
}
