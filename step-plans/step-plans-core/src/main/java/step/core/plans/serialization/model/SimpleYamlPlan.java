package step.core.plans.serialization.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SimpleYamlPlan {
	private String name;
	private ObjectNode root;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ObjectNode getRoot() {
		return root;
	}

	public void setRoot(ObjectNode root) {
		this.root = root;
	}
}
