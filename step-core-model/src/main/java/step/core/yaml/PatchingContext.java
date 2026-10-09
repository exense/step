package step.core.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.yaml.deserialization.AutomationPackageUpdateException;
import step.core.yaml.deserialization.PatchingParserDelegate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PatchingContext {
    private static final Logger logger = LoggerFactory.getLogger(PatchingContext.class);
    private final String sourceLocation;
    private final CopyOnWriteArrayList<String> initialLines;

    private final ObjectMapper mapper;

    protected final ConcurrentNavigableMap<ChunkBounds, PatchableYamlModel> chunks = new ConcurrentSkipListMap<>();

    public PatchingContext() {
        this(new ObjectMapper());
    }

    public PatchingContext(ObjectMapper mapper) {
        this("", "", mapper);
    }

    public PatchingContext(String sourceLocation, String yaml, ObjectMapper mapper) {
        this.sourceLocation = Objects.requireNonNull(sourceLocation);
        this.initialLines = new CopyOnWriteArrayList<>(Objects.requireNonNull(yaml).lines().toList());
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public String getChunk(PatchableYamlModel entity) {
        return getChunkBounds(entity).map(this::getChunk)
            .orElse(null);
    }

    private Optional<ChunkBounds> getChunkBounds(PatchableYamlModel entity) {
        // this is not terribly efficient as it will be O(n), but we're talking small amounts of data
        return chunks.entrySet().stream()
            .filter(entry -> entry.getValue().equals(entity))
            .findFirst()
            .map(Map.Entry::getKey);
    }

    private String getChunk(ChunkBounds bounds) {
        return String.join("\n", getLines(bounds)) + "\n";
    }

    private List<String> getLines(ChunkBounds bounds) {
        return initialLines.subList(bounds.startLineNumber - 1, bounds.endLineNumber);
    }

    public boolean chunkClaimed(PatchableYamlModel entity) {
        return getChunkBounds(entity).isPresent();
    }

    public String getUnclaimedChunkBefore(PatchableYamlModel item) {
        Optional<ChunkBounds> chunkBoundsOptional = getChunkBounds(item);
        if (chunkBoundsOptional.isEmpty()) {
            return "";
        }
        ChunkBounds chunkBounds = chunkBoundsOptional.get();
        ChunkBounds chunkBefore = chunks.lowerKey(chunkBounds);
        if (chunkBefore == null) {
            if (chunkBounds.startLineNumber > 1) {
                return getChunk(new ChunkBounds(1, chunkBounds.startLineNumber - 1, ChunkBounds.Portion.BODY));
            }
            return "";
        }
        if (chunkBefore.endLineNumber + 1 < chunkBounds.startLineNumber) {
            return getChunk(new ChunkBounds(chunkBefore.endLineNumber() + 1, chunkBounds.startLineNumber - 1, ChunkBounds.Portion.BODY));
        }
        return "";
    }

    /**
     * @return whether the entity starts a line of its own with the item indicator ("- "), as the items of a block
     * sequence do. The items of a flow sequence ("[a, b]") share their lines with the sequence and with each other:
     * their claim may have been overridden by the one of the sequence, and their lines are not theirs alone
     */
    public boolean claimedAsBlockSequenceItem(PatchableYamlModel entity) {
        return getChunkBounds(entity)
            .map(bounds -> initialLines.get(bounds.startLineNumber - 1).stripLeading().startsWith("-"))
            .orElse(false);
    }


    public record ChunkBounds(int startLineNumber, int endLineNumber, Portion portion) implements Comparable<ChunkBounds> {

        public enum Portion {
            HEAD,
            BODY
        }

        private static final Comparator<ChunkBounds> COMPARATOR = Comparator
            .comparingInt(ChunkBounds::startLineNumber) // lower startLine first
            .thenComparing(Comparator.comparingInt(ChunkBounds::endLineNumber).reversed()); // larger endLine (i.e. larger chunk) first
        private static final Comparator<ChunkBounds> COMPARATOR_WITH_PORTION = Comparator
            .comparing(ChunkBounds::portion) // first portion
            .thenComparing(
                Comparator.comparingInt(ChunkBounds::startLineNumber) // then startLine first
                    .thenComparing(Comparator.comparingInt(ChunkBounds::endLineNumber).reversed())); // larger endLine (i.e. larger chunk) first

        @Override
        public int compareTo(ChunkBounds that) {
            return COMPARATOR.compare(this, that);
        }

        public boolean encompasses(ChunkBounds inner) {
            return inner.startLineNumber >= this.startLineNumber && inner.endLineNumber <= this.endLineNumber;
        }
    }

    public String getCurrentYaml() {
        List<ChunkBounds> allBounds = getAllOuterBounds();
        StringBuilder yaml = new StringBuilder();
        for (ChunkBounds bound : allBounds) {
            PatchableYamlModel patchableYamlModel = chunks.get(bound);
            if (patchableYamlModel == null) {
                // unclaimed, return original lines
                yaml.append(getChunk(bound));
            } else {
                // claimed, return whatever that patchable currently thinks its content is,
                // using its original indentation
                String indent = detectIndent(initialLines.get(bound.startLineNumber - 1)); // we only need the first line
                yaml.append(patchableYamlModel.getCurrentYaml(indent));
            }
        }
        return yaml.toString();
    }

    /**
     * @return a sorted list of bounds, encompassing the entire original
     * file. Note this only returns top-level bounds, not bounds that are
     * encompassed by other bounds. For instance: returns list bounds,
     * but not bounds of items inside those lists. This also returns
     * bounds for lines that are NOT claimed by anything.
     */
    private List<ChunkBounds> getAllOuterBounds() {
        List<ChunkBounds> allBounds = getClaimedOuterBounds();
        List<ChunkBounds> unclaimedBounds = new ArrayList<>();
        // claimedBounds is sorted, we need to fill the gaps
        int startLineNumber = 1;
        ChunkBounds.Portion portion = ChunkBounds.Portion.HEAD;
        for (ChunkBounds bound : allBounds) {
            if (bound.startLineNumber > startLineNumber) {
                unclaimedBounds.add(new ChunkBounds(startLineNumber, bound.startLineNumber - 1, portion));
            }
            startLineNumber = bound.endLineNumber + 1;
            portion = bound.portion;
        }
        allBounds.addAll(unclaimedBounds);
        unclaimedBounds.clear(); // not needed anymore, might as well free it
        allBounds.sort(ChunkBounds.COMPARATOR);
        // The lines after the last claimed chunk, or the whole file when nothing is claimed in it, for instance when it
        // only holds fields no model is registered for
        int lastClaimedLineNumber = allBounds.isEmpty() ? 0 : allBounds.getLast().endLineNumber;
        if (lastClaimedLineNumber < initialLines.size()) {
            allBounds.add(new ChunkBounds(lastClaimedLineNumber + 1, initialLines.size(), ChunkBounds.Portion.BODY));
        }
        allBounds.sort(ChunkBounds.COMPARATOR_WITH_PORTION);
        return allBounds;
    }

    /**
     *
     * @return a sorted list of (only) the outer bounds of claimed lines, i.e. the chunk bounds
     * that are owned by some PatchableYamlModel. Inner claims (e.g. actual objects inside a list) are disregarded
     */
    private List<ChunkBounds> getClaimedOuterBounds() {
        List<ChunkBounds> allBounds = chunks.keySet().stream().toList();
        ChunkBounds lastBound = null;
        List<ChunkBounds> outerBounds = new ArrayList<>();
        for (ChunkBounds bound : allBounds) {
            if (lastBound != null) {
                if (lastBound.encompasses(bound)) {
                    continue;
                }
            }
            lastBound = bound;
            outerBounds.add(bound);
        }
        return outerBounds;
    }

    private String serializeUnindented(Object entity) {
        try {
            return mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new AutomationPackageUpdateException("Error Serializing YAML object", e);
        }
    }

    /*
    Note for this and following methods: because of how YAML works, contextIndent could
    be simply a string of spaces (" "), but it also could contain a dash if the item
    is contained in a list ("   - "). Only the first line needs the list marker, all others
    need to be aligned and consist only of spaces.
     */
    public String serialize(Object entity, String contextIndent) {
        return indent(serializeUnindented(entity), contextIndent);
    }

    String indent(String chunk, String contextIndent) {
        if (chunk == null || chunk.isEmpty()) {
            return chunk;
        }
        String onlyIndent = contextIndent.replace('-', ' ');
        AtomicBoolean firstLine = new AtomicBoolean(true);
        return chunk.lines()
            .map(line -> firstLine.getAndSet(false) ? contextIndent + line : line.isEmpty() ? line : onlyIndent + line)
            .collect(Collectors.joining("\n", "", "\n"));
    }

    public String reindent(String chunk, String contextIndent) {
        if (chunk == null || chunk.isEmpty()) {
            return chunk;
        }
        String existingIndent = detectIndent(chunk);
        if (existingIndent.equals(contextIndent)) {
            return chunk;
        }
        String unindented = stripIndent(chunk, existingIndent.length());
        return indent(unindented, contextIndent);
    }

    private String stripIndent(String chunk, int length) {
        return chunk.lines()
            .map(l -> {
                // handle potential comments which may not be properly aligned.
                if (l.trim().startsWith("#")) {
                    if (l.indexOf('#') < length) {
                        return l.trim();
                    }
                }
                // short/empty lines
                if (l.length() <= length) {
                    return "";
                }
                // any other line - chop off indent
                return l.substring(length);
            })
            .collect(Collectors.joining("\n", "", "\n"));
    }

    private String detectIndent(String chunk) {
        // chunk is guaranteed to be non-empty; we don't care how many lines this has, we only need to look at the first one.
        int pos = 0;
        int len = chunk.length();

        // Leading spaces
        while (pos < len && chunk.charAt(pos) == ' ') {
            pos++;
        }

        // Optional list indicator (dash) and following spaces
        if (pos < len && chunk.charAt(pos) == '-') {
            pos++;
            // 3. Consume spaces after the dash
            while (pos < len && chunk.charAt(pos) == ' ') {
                pos++;
            }
        }
        return chunk.substring(0, pos);
    }


    public ChunkBounds claimChunk(JsonLocation startLocation, PatchingParserDelegate parser, PatchableYamlModel entity) {
        PatchingParserDelegate.TokenLocationPair pair = parser.getTokenLocationPair();

        // Scan back to first token which does NOT indicate the end of an object or array,
        // unless it is an empty object or array.
        // This is so far the most generic way to find the last non-empty and non-comment line of an object
        // of an empty object respectively array.
        while ((pair.token() == JsonToken.END_OBJECT && pair.previous().token() != JsonToken.START_OBJECT)
            || (pair.token() == JsonToken.END_ARRAY && pair.previous().token() != JsonToken.START_ARRAY)) {
            pair = pair.previous();
        }
        int endLine = pair.location().getLineNr();

        // Only exception found ist when the last scalar is a string defined via one of the block
        // definition methods (|, |-, |+, >, >-, >+). In this case, there is no ending quote indicating the
        // end of the string on its proper ending line, and so the parser location after parsing the
        // VALUE_STRING token ends up on the next non-comment, non-empty line.
        if (pair.token() == JsonToken.VALUE_STRING
            && pair.location().getColumnNr() == 1 && startLocation.getLineNr() < endLine) {

            endLine--;
        }
        return claimChunk(startLocation.getLineNr(), endLine, entity);
    }

    public ChunkBounds claimChunk(int startLineNumber, int endLineNumber, PatchableYamlModel entity) {
        ChunkBounds bounds = new ChunkBounds(startLineNumber, endLineNumber, ChunkBounds.Portion.BODY);
        chunks.put(bounds, entity);
        return bounds;
    }


    public ChunkBounds appendAndClaim(PatchableYamlModel entity, String chunk) {
        return appendAndClaim(entity, chunk, ChunkBounds.Portion.BODY);
    }

    public ChunkBounds appendAndClaim(PatchableYamlModel entity, String chunk, ChunkBounds.Portion portion) {
        List<String> lines = chunk.lines().toList();
        // yes, initialLines won't be so "initial" anymore ;-)
        initialLines.addAll(lines);
        int endLine = initialLines.size();
        int startLine = endLine - lines.size() + 1;
        ChunkBounds bounds = new ChunkBounds(startLine, endLine, portion);
        chunks.put(bounds, entity);
        return bounds;
    }


    public void replaceEntity(PatchableYamlModel oldPatchable, PatchableYamlModel newPatchable) {
        newPatchable.setPatchingContext(this);
        newPatchable.setModified();
        getChunkBounds(oldPatchable).ifPresent(bounds -> chunks.put(bounds, newPatchable));
    }
}
