package step.grid.io.helper;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import step.grid.io.ParserException;

public class DocumentParser {

	private DocumentBuilderFactory documentBuilderfactory;
	
	public DocumentParser() {
		super();
		
		documentBuilderfactory = DocumentBuilderFactory.newInstance();
		documentBuilderfactory.setNamespaceAware(true);
		documentBuilderfactory.setValidating(false);
	}
	
	public Document parse(String xml) throws ParserException {
		Document document;
		List<String> errorContainer = new ArrayList<>();
		try {
			document = getDocumentBuilder(errorContainer).parse(new InputSource(new StringReader(xml)));			
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error occurred while parsing:" + xml,e);
		} catch (SAXException e) {
			throw new ParserException("Unable to parse: " + e.getMessage());
		}
		if(errorContainer.size()>0) {
			StringBuilder errorMsg = new StringBuilder();
			for(String error:errorContainer) {
				errorMsg.append(error+" \n");
			}
			throw new ParserException(errorMsg.toString());
		}
	
		return document;
	}
	
	private DocumentBuilder getDocumentBuilder(final List<String> errorContainer) {
		DocumentBuilder documentBuilder;
		
		ErrorHandler myErrorHandler = new ErrorHandler()
		{
		    public void fatalError(SAXParseException exception)
		    throws SAXException
		    {}
		    
		    public void error(SAXParseException exception)
		    throws SAXException
		    {
		    	errorContainer.add(exception.getLocalizedMessage());
		    }
	
		    public void warning(SAXParseException exception)
		    throws SAXException
		    {}
		};
		
	
		try {
			documentBuilder = documentBuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Unable to initialize document builder.",e);
	
		}
		documentBuilder.setErrorHandler(myErrorHandler);
		return documentBuilder;
	}
}
