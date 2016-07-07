package step.grid.io;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import step.grid.io.helper.DocumentParser;

public abstract class AdapterMessageBuilder<T extends AdapterMessage> {

	protected T message;
	
	protected Document document = null;
	
	private DocumentParser parser;
	
	protected DocumentBuilder documentBuilder = null;

	protected DocumentBuilderFactory documentBuilderfactory = null;
	
	protected AdapterMessageBuilder(T message) {
		super();
		this.message = message;
		
		documentBuilderfactory = DocumentBuilderFactory.newInstance();
		try {
			documentBuilder = documentBuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public AdapterMessageBuilder<T> setPayload(String payload) throws ParserException {
		initParserIfNeeded();
		document = parser.parse(payload);
		return this;
	}
	
	public void setPayload(Document payload) {
		document = payload;
	}
	
	private void initParserIfNeeded() {
		if(parser==null) {
			parser = new DocumentParser();
		}
	}
	
	public T build() {
		if(document==null) {
			document = documentBuilder.newDocument();
		}
		message.setPayload(document);
		return message;
	}
	
	public void createDocument(String tagName) {
		document = documentBuilder.newDocument();
		Element outputElement = document.createElement(tagName);
		document.appendChild(outputElement);
	}
	
	public void setPayloadAttribute(String paramName, String value) {
		document.getDocumentElement().setAttribute(paramName, value);
	}
}
