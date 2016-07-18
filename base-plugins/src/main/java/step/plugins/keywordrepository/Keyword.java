package step.plugins.keywordrepository;

import java.util.HashMap;
import java.util.Map;

import step.commons.activation.ActivableObject;

public class Keyword extends ActivableObject {
	
	private String name;
	
	private KeywordType type = KeywordType.REMOTE;
	
	private Map<String, String> attributes = new HashMap<>();
	
	private Map<String, String> selectionPatterns = new HashMap<>();
	
	private Boolean hasSchema = true;

	public Keyword() {
		super();
	}

	public Keyword(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public KeywordType getType() {
		return type;
	}

	public void setType(KeywordType type) {
		this.type = type;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setSelectionPatterns(Map<String, String> selectionPatterns) {
		this.selectionPatterns = selectionPatterns;
	}

	public void addAttribute(String key, String value) {
		attributes.put(key, value);
	}

	public Map<String, String> getSelectionPatterns() {
		return selectionPatterns;
	}
	
	public void addSelectionPattern(String key, String value) {
		selectionPatterns.put(key, value);
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasSchema() {
		return hasSchema;
	}

	public void setHasSchema(boolean useSchema) {
		this.hasSchema = useSchema;
	}

}
