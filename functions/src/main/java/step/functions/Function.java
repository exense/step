package step.functions;

import java.util.Map;

import org.bson.types.ObjectId;

public class Function {
	
	ObjectId _id;
	
	Map<String, String> attributes;
	
	String handlerChain;

	Map<String, String> handlerProperties;

	public ObjectId getId() {
		return _id;
	}

	public void setId(ObjectId id) {
		this._id = id;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Map<String, String> getHandlerProperties() {
		return handlerProperties;
	}

	public void setHandlerProperties(Map<String, String> handlerProperties) {
		this.handlerProperties = handlerProperties;
	}

	public String getHandlerChain() {
		return handlerChain;
	}

	public void setHandlerChain(String handlerChain) {
		this.handlerChain = handlerChain;
	}

}
