package step.grid.io;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;

@XmlRootElement
public class Input extends AdapterMessage {
	
	@XmlAttribute
	String userID;
		
	@XmlElement
	Map<String, String> parameters;
	
	protected Input() {
		super();
	}

	public String getUserID() {
		return userID;
	}

	protected void setUserID(String userID) {
		this.userID = userID;
	}

	public String getKeyword() {
		Element docElement = payload.getDocumentElement();
		if(payload!=null) {
			return docElement.getLocalName()!=null?docElement.getLocalName():docElement.getTagName();
		} else {
			return null;
		}
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	protected void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	public String getParameter(String paramName) {
		return getParameters().get(paramName);	
	}
	
	public String getValue(String key) {
		String value = getPayloadAttribute(key);
		if(value==null) {
			value = getParameter(key);
		}
		return value;	
	}
	
	public String getValue(String key, String defaultValue) {
		String value = getValue(key);
		return value!=null?value:defaultValue;
	}
	
	public boolean existsValue(String key) {
		return getValue(key)!=null;
	}
}
