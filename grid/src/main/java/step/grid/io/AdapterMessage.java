package step.grid.io;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import step.grid.io.helper.PayloadXmlAdapter;

public class AdapterMessage {
	
	@XmlElement
	@XmlJavaTypeAdapter(PayloadXmlAdapter.class)
	protected Document payload;

	public AdapterMessage() {
		super();
	}

	public Document getPayload() {
		return payload;
	}

	protected void setPayload(Document document) {
		this.payload = document;
	}
	
	public String getPayloadAttribute(String paramName) {
		if(payload!=null && payload.getDocumentElement()!=null && payload.getDocumentElement().hasAttribute(paramName)) {
			return payload.getDocumentElement().getAttribute(paramName);
		} else {
			return null;
		}
	}
	
	public Map<String, String> getPayloadAttributes() {
		Map<String, String> result = new HashMap<>();
		if(payload!=null && payload.getDocumentElement()!=null) {
			NamedNodeMap map = payload.getDocumentElement().getAttributes();
			for(int i=0; i<map.getLength(); i++) {
				Node node = map.item(i);
				if(node.getNodeType()==Node.ATTRIBUTE_NODE) {
					result.put(node.getNodeName(), node.getNodeValue());
				}
			}
		}
		return result;
	}
}