package step.grid.io.helper;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PayloadXmlAdapter extends XmlAdapter<Object, Document> {

	private DocumentBuilder documentBuilder;

	public PayloadXmlAdapter() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			documentBuilder = dbf.newDocumentBuilder();
		} catch (Exception e) {
			// TODO - Handle Exception
		}
	}

	@Override
	public Document unmarshal(Object v) throws Exception {
		Element e = (Element) v;
		Element c = (Element) e.getFirstChild();
		Document d = documentBuilder.newDocument();
		d.appendChild(d.importNode(c, true));
		return d;
	}

	@Override
	public Object marshal(Document v) throws Exception {
		Document d = documentBuilder.newDocument();
		Element e = d.createElement("payload");
		if(v!=null && v.getDocumentElement()!=null) {
			e.appendChild(d.importNode(v.getDocumentElement(), true));
		}
		return e;
	}

}
