package step.grid.io.helper;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class DocumentTransformer {
	
	public TransformerFactory tf;

	public DocumentTransformer() {
		super();
		tf = TransformerFactory.newInstance();
	}

	public String transform(Document document) {
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			String documentAsString = writer.getBuffer().toString().replaceAll("\n|\r", "");
			return documentAsString;
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}
}
