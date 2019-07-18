package step.core.deployment;

public class MoveArtefactData {
    private String id;
    private String text;
    private String oldParent;
    private String parent;
    private int position;
    
    public MoveArtefactData() {
        super();
    }
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getOldParent() {
        return oldParent;
    }
    public void setOldParent(String old_parent) {
        this.oldParent = old_parent;
    }
    public String getParent() {
        return parent;
    }
    public void setParent(String parent) {
        this.parent = parent;
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
        this.position = position;
    }   
}
