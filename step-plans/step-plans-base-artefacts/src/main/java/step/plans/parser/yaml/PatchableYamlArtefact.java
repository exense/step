package step.plans.parser.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonLocation;

public abstract class PatchableYamlArtefact {
    
    private int startOffset = -1;
    private int startColumn = -1;
    private int endOffset = -1;

    @JsonIgnore
    public void setPatchingBounds(JsonLocation startLocation, JsonLocation endLocation) {
        startOffset = (int) startLocation.getCharOffset();
        endOffset = (int) endLocation.getCharOffset();
        startColumn = startLocation.getColumnNr() -1;
    }
    
    @JsonIgnore
    public int getStartOffset(){
        return startOffset;
    }

    @JsonIgnore
    public int getIndent() {
        return startColumn;
    }

    @JsonIgnore
    public int getEndOffset() {
        return endOffset;
    }


    public void setPatchingBounds(PatchableYamlArtefact newBoundedArtefact) {
        startOffset = newBoundedArtefact.startOffset;
        startColumn = newBoundedArtefact.startColumn;
        endOffset = newBoundedArtefact.endOffset;
    }
    
    @JsonIgnore
    abstract public String getCollectionName();
}
