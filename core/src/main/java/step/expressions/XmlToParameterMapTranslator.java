package step.expressions;

import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class XmlToParameterMapTranslator {
		
	public HashMap<String,String> getParameterMap(Document document) {	
		if(document!=null) {
			Element root = document.getDocumentElement();
			if(root!=null) {
				return populateMap(root);				
			}
		}
		return new HashMap<>();
	}
	
	private Vector<Node> getChildElements(Node node) {
		Vector<Node> elements = new Vector<Node>();
		for (int i = 0; i < node.getAttributes().getLength(); i++) {
			if (node.getAttributes().item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
				elements.add((Node)node.getAttributes().item(i));
			}
		}
		
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elements.add((Node) list.item(i));
			}
		}
		return elements;
	}
	
	private HashMap<String,String> populateMap(Node node) {
		Vector<Node> children = getChildElements(node);
		
		HashMap<String, String> map = new HashMap<>();
		
		for(Node child:children) {
			String value = null;
			String key = null;
			if(child.getNodeType()==Node.ATTRIBUTE_NODE) {
				key = child.getNodeName();
				value = child.getNodeValue();
			} else if (child.getNodeType() == Node.ELEMENT_NODE) {				
				if(child.getChildNodes().getLength()==1 && child.getFirstChild() instanceof Text) {
					key = child.getNodeName();
					value = ((Text)child.getFirstChild()).getTextContent();
				}
			}
			if(key!=null) {
				map.put(key, value);
			}
		}
		
		return map;
	}
}
