package step.core.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.yaml.deserialization.AutomationPackageUpdateException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        this(null, "", mapper);
    }

    public PatchingContext(String sourceLocation, String yaml, ObjectMapper mapper) {
        this.sourceLocation = sourceLocation;
        this.initialLines = new CopyOnWriteArrayList<>(yaml.lines().toList());
        this.mapper = mapper;
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

    public record ChunkBounds(int startLineNumber, int endLineNumber) implements Comparable<ChunkBounds>

    {
        private static final Comparator<ChunkBounds> COMPARATOR = Comparator
            .comparingInt(ChunkBounds::startLineNumber) // lower startLine first
            .thenComparing(Comparator.comparingInt(ChunkBounds::endLineNumber).reversed()); // larger endLine (i.e. larger chunk) first

        @Override
        public int compareTo (ChunkBounds that){
        return COMPARATOR.compare(this, that);
    }

        public boolean encompasses (ChunkBounds inner){
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
        for (ChunkBounds bound : allBounds) {
            if (bound.startLineNumber > startLineNumber) {
                unclaimedBounds.add(new ChunkBounds(startLineNumber, bound.startLineNumber - 1));
            }
            startLineNumber = bound.endLineNumber + 1;
        }
        allBounds.addAll(unclaimedBounds);
        unclaimedBounds.clear(); // not needed anymore, might as well free it
        allBounds.sort(ChunkBounds.COMPARATOR);
        if (!allBounds.isEmpty() && allBounds.getLast().endLineNumber < initialLines.size()) {
            allBounds.add(new ChunkBounds(allBounds.getLast().endLineNumber + 1, initialLines.size()));
        }
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
            return mapper.writeValueAsString(entity)
                .replaceFirst("^---\\s*\\n", "")
                .trim();
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
            .map(line -> firstLine.getAndSet(false) ? contextIndent + line : onlyIndent + line)
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


    public ChunkBounds claimChunk(JsonLocation startLocation, JsonLocation endLocation, PatchableYamlModel entity) {
        int startLineNumber = startLocation.getLineNr();
        int endLineNumber = endLocation.getLineNr();
        String startLine = initialLines.get(startLineNumber - 1);
        if (startLocation.getColumnNr() >= startLine.length()) {
            // sometimes the start is at the correct line, sometimes it's at the end (in fact, AFTER the end) of some previous line
            PatchableYamlModel.StartingLineDeterminationStrategy strategy = entity.getStartingLineDeterminationStrategy();
            logger.debug("{}: entity of {} seems to start at end of line {} (columnNr={}, lineLength={}), determining correct entity start line (strategy={})", sourceLocation,
                entity.getClass(), startLocation.getLineNr(), startLocation.getColumnNr(), startLine.length(), strategy);
            if (strategy == PatchableYamlModel.StartingLineDeterminationStrategy.NEXT_CONTENT_LINE) {
                boolean found = false;
                for (int l = startLineNumber; l < initialLines.size(); l++) {
                    String candidate = initialLines.get(l).trim();
                    if (!candidate.isEmpty() && !candidate.startsWith("#")) {
                        startLineNumber = l + 1;
                        logger.debug("Using first non-empty, non-comment line (lineNumber={}, line content={})", startLineNumber, candidate);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.debug("Unable to determine correct start line for entity, keeping original line number!");
                }
            }
        }
        ChunkBounds bounds = new ChunkBounds(startLineNumber, endLineNumber);
        chunks.put(bounds, entity);
        return bounds;
    }

    public ChunkBounds appendAndClaim(PatchableYamlModel entity, String chunk) {
        List<String> lines = chunk.lines().toList();
        // yes, initialLines won't be so "initial" anymore ;-)
        initialLines.addAll(lines);
        int endLine = initialLines.size();
        int startLine = endLine - lines.size() + 1;
        ChunkBounds bounds = new ChunkBounds(startLine, endLine);
        chunks.put(bounds, entity);
        return bounds;
    }


    public void replaceEntity(PatchableYamlModel oldPatchable, PatchableYamlModel newPatchable) {
        newPatchable.setPatchingContext(this);
        newPatchable.setModified();
        getChunkBounds(oldPatchable).ifPresent(bounds -> chunks.put(bounds, newPatchable));
    }
}
